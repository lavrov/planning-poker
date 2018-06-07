package client

import cats.effect.IO
import com.github.lavrov.poker._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import outwatch.{Handler, Sink}
import outwatch.http.Http
import io.circe.syntax._
import io.circe.generic.auto._

class PlanningPokerApp(endpoints: Endpoints, initState: PlanningPokerApp.AppState) {
  import PlanningPokerApp._

  def createStore: IO[Store] =
    for {
      store0 <- Store.create(initState, reducer)
      store1 = WebSocketSupport.enhance(Store(store0.source, store0.sink), subscriptions)
      store2 <- Routing.enhance(store1)
    }
    yield store2

  def reducer(state: AppState, action: Action): (AppState, Option[IO[Action]]) = action match {
    case Action.ChangePage(page) =>
      page match {
        case _: Page.SignedInUser if state.user.isEmpty =>
          state.copy(redirectOnSignIn = Some(page)) -> Some(Routing.navigate(Page.SignIn()))
        case Page.Session(id, _) if !state.session.exists(_.id == id) =>
          state.copy(page = page, session = Some(CurrentPlanningSession(id, None))) -> None
        case _ =>
          state.copy(page = page) -> None
      }
    case Action.RequestSession() =>
      state -> Some {
        val request = Http.Request(endpoints.session.create)

        Http.single(request, Http.Post)
          .attempt
          .map {
            case Right(response) =>
              if (response.status == 200) {
                println("Received " + response.body)
                Action.ReceiveSession(io.circe.parser.decode[String](response.body.toString).right.get)
              }
              else {
                println(s"Bad response ${response.status}")
                Action.Noop
              }
            case Left(error) =>
              println(s"Error while creating session. $error")
              Action.Noop
          }
      }
    case Action.ReceiveSession(sessionId) =>
      state.copy(session = Some(CurrentPlanningSession(sessionId, None))) -> Some {
        Routing.navigate(Page.Session(sessionId)).map(_ => Action.Noop)
      }
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
      state.copy(user = None) -> Some {
        Routing.navigate(Page.Home)
      }
    case Action.UpdatePlanningSession(session) =>
      state.copy(session = state.session.map(_.copy(planningSession = Some(session)))) ->
      state.user.collect {
        case u if !session.participants.keySet(u.id) =>
          IO pure Action.SendPlanningSessionAction(PlanningSession.Action.AddPlayer(u))
      }
    case Action.SendPlanningSessionAction(psAction) =>
      println(s"Reducer SendPlanningSessionAction($psAction)")
      println(s"And state.session is ${state.session}")
      state -> state.session.map(_.id).zip(state.user).headOption.map { case (sessionId, user) =>
        WebSocketSupport.send(endpoints.session.ws(sessionId, user.id), psAction.asJson.noSpaces)
      }
    case Action.Noop =>
      state -> None
  }

  def subscriptions(state: AppState): Option[Sub.WebSocket] = {
    import io.circe.parser.decode, io.circe.generic.auto._
    state.session.zip(state.user).headOption.map { case (s, u) =>
      Sub.WebSocket(
        endpoints.session.ws(s.id, u.id),
        msg =>
          Action.UpdatePlanningSession(decode[PlanningSession](msg).right.get)
      )
    }
  }
}

object PlanningPokerApp {

  case class AppState(
      page: Page,
      user: Option[Participant],
      session: Option[CurrentPlanningSession] = None,
      redirectOnSignIn: Option[Page] = None
  )

  case class CurrentPlanningSession(
      id: String,
      planningSession: Option[PlanningSession]
  )

  sealed trait Action
  object Action {
    case class SignIn(name: String) extends Action
    case object SignOut extends Action
    case class RequestSession() extends Action
    case class ReceiveSession(sessionId: String) extends Action
    case class SendPlanningSessionAction(action: PlanningSession.Action) extends Action
    case class UpdatePlanningSession(session: PlanningSession) extends Action
    case class ChangePage(page: Page) extends Action
    case object Noop extends Action
  }

  trait Sub
  object Sub {
    case class WebSocket(url: String, actionFn: String => Action) extends Sub
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
