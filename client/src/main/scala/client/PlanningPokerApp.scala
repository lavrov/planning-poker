package client

import cats.effect.IO
import cats.syntax.apply._
import com.github.lavrov.poker._, Formats._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import outwatch.{Handler, Sink}
import outwatch.http.Http
import io.circe.parser._

class PlanningPokerApp(endpoints: Endpoints, initState: PlanningPokerApp.AppState) {
  import PlanningPokerApp._

  def createStore: IO[Store] =
    for {
      maybeUser <- LocalStorage.read
      stateWithUser = initState.copy(user = maybeUser)
      store0 <- Store.create(stateWithUser, reducer)
      store1 = WebSocketSupport.enhance(Store(store0.source, store0.sink), subscriptions)
      store2 <- Routing.enhance(store1)
    }
    yield store2

  def reducer(state: AppState, action: Action): (AppState, Option[IO[Action]]) = action match {
    case Action.ChangePage(page) =>
      page match {
        case _: Page.SignedInUser if state.user.isEmpty =>
          state.copy(redirectOnSignIn = Some(page)) -> Some(Routing.navigate(Page.SignIn()))
        case page@Page.Session(id, _) =>
          state.copy(page = page) -> Some {
            if (state.page == page && state.session.nonEmpty)
              IO.pure(Action.Noop)
            else
              Http.single(
                Http.Request(endpoints.session.get(id)),
                Http.Get
              )
              .attempt
              .map {
                case Right(response) =>
                  response.status match {
                    case 200 =>
                      val session = decode[PlanningSession](response.body.toString).right.get
                      Action.ReceiveSession(Some(CurrentPlanningSession(id, session)))
                    case 404 =>
                      Action.ReceiveSession(None)
                  }
                case Left(error) =>
                  Action.ReceiveSession(None)
              }
          }
        case _ =>
          state.copy(page = page) -> None
      }
    case Action.RequestSession() =>
      state -> Some {
        val request = Http.Request(endpoints.session.create)

        Http.single(request, Http.Post)
          .attempt
          .flatMap {
            case Right(response) =>
              if (response.status == 200) {
                println("Received " + response.body)
                val sessionId = decode[String](response.body.toString).right.get
                Routing.navigate(Page.Session(sessionId))
              }
              else {
                println(s"Bad response ${response.status}")
                IO pure Action.Noop
              }
            case Left(error) =>
              println(s"Error while creating session. $error")
              IO pure Action.Noop
          }
      }
    case Action.ReceiveSession(sessionOpt) =>
      state.copy(session = Some apply sessionOpt.toRight("Session doesn't exist")) -> None
    case Action.UpdateSocketStatus(status) =>
      state.copy(session =
        state.session.map(_.right.map(session => session.copy(status = status)))) -> None
    case Action.SignIn(userName) =>
      val id = java.util.UUID.randomUUID().toString
      val u = Participant(id, userName)
      state.copy(user = Some(u), redirectOnSignIn = None) -> Some {
        for {
          _ <- LocalStorage.persist(u)
          _ <- Routing.navigate(state.redirectOnSignIn getOrElse Page.Sessions())
        }
        yield Action.Noop
      }
    case Action.SignOut =>
      initState -> Some {
        LocalStorage.clear *> Routing.navigate(Page.Home)
      }
    case Action.UpdatePlanningSession(session) =>
      state.copy(session = state.session.map(_.right.map(_.copy(planningSession = session)))) ->
      state.user.collect {
        case u if !session.participants.keySet(u.id) =>
          IO pure Action.SendPlanningSessionAction(PlanningSession.Action.AddPlayer(u))
      }
    case Action.SendPlanningSessionAction(psAction) =>
      state -> state.session.flatMap(_.right.toOption).zip(state.user).headOption.map { case (currentSession, user) =>
        WebSocketSupport.send(endpoints.session.ws(currentSession.id, user.id), Protocol.ClientMessage.SessionAction(psAction))
      }
    case Action.Noop =>
      state -> None
  }

  def subscriptions(state: AppState): Option[Sub.WebSocket] = {
    state.session.flatMap(_.right.toOption).zip(state.user).headOption.map { case (s, u) =>
      Sub.WebSocket(
        endpoints.session.ws(s.id, u.id),
        msg => {
          Action.UpdatePlanningSession(msg.payload)
        },
        Some(Action.UpdateSocketStatus(true)),
        Some(Action.UpdateSocketStatus(false)),
      )
    }
  }
}

object PlanningPokerApp {

  case class AppState(
      page: Page,
      user: Option[Participant] = None,
      session: Option[Either[String, CurrentPlanningSession]] = None,
      redirectOnSignIn: Option[Page] = None
  )

  case class CurrentPlanningSession(
      id: String,
      planningSession: PlanningSession,
      status: Boolean = false
  )

  sealed trait Action
  object Action {
    case class SignIn(name: String) extends Action
    case object SignOut extends Action
    case class RequestSession() extends Action
    case class ReceiveSession(session: Option[CurrentPlanningSession]) extends Action
    case class UpdateSocketStatus(connected: Boolean) extends Action
    case class SendPlanningSessionAction(action: PlanningSession.Action) extends Action
    case class UpdatePlanningSession(session: PlanningSession) extends Action
    case class ChangePage(page: Page) extends Action
    case object Noop extends Action
  }

  trait Sub
  object Sub {
    case class WebSocket(
        url: String,
        actionFn: Protocol.ServerMessage.SessionUpdated => Action,
        connectedAction: Option[Action],
        disconnectedAction: Option[Action])
      extends Sub
  }

  case class Store(source: Observable[AppState], sink: Sink[Action])
  object Store {
    def create(initialState: AppState, reducer: (AppState, Action) => (AppState, Option[IO[Action]])): IO[Store] =
    for {
      handler <- Handler.create[Action]
    }
    yield {
      def foldState(state: AppState, action: Action): Observable[AppState] = {
        val (newState, nextAction) = reducer(state, action)
          nextAction match {
            case Some(nextActionIO) =>
              Observable(newState) ++
              Observable.fromIO(nextActionIO)
                .flatMap { a =>
                  foldState(newState, a)
                }
            case None =>
              Observable(newState)
          }
        }
      val sink: Sink[Action] = handler
      val source: Observable[AppState] = handler
        .flatScan(initialState)(foldState)
        .startWith(Seq(initialState))
        .share
      Store(source, sink)
    }
  }
}
