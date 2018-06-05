package client

import cats.effect.IO
import com.github.lavrov.poker._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import outwatch.{Handler, Sink}
import outwatch.http.Http
import io.circe.syntax._, io.circe.generic.auto._

class PlanningPokerApp(endpoints: Endpoints, initState: PlanningPokerApp.AppState) {
  import PlanningPokerApp._

  def createStore: IO[Store] =
    for {
      store0 <- Store.create(initState, reducer)
      store1 = WebSocketSupport.enhance(Store(store0.source, store0.sink), subscriptions)
    }
    yield store1

  def reducer(state: AppState, action: Action): (AppState, Option[IO[Action]]) = action match {
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
      state.copy(session = Some(CurrentPlanningSession(sessionId, None))) -> state.user.map { u =>
        IO.pure {
          Action.SendPlanningSessionAction(PlanningSession.Action.AddPlayer(u))
        }
      }
    case Action.Login(userName) =>
      state.copy(user = Some(Participant(userName, userName))) -> None
    case Action.UpdatePlanningSession(session) =>
      state.copy(session = state.session.map(_.copy(planningSession = Some(session)))) -> None
    case Action.SendPlanningSessionAction(psAction) =>
      println(s"Reducer SendPlanningSessionAction($psAction)")
      println(s"And state.session is ${state.session}")
      state -> state.session.map(_.id).map { sessionId =>
        WebSocketSupport.send(endpoints.session.ws(sessionId), psAction.asJson.noSpaces)
      }
    case Action.Noop =>
      state -> None
  }

  def subscriptions(state: AppState): Option[Sub.WebSocket] = {
    import io.circe.parser.decode, io.circe.generic.auto._
    state.session.map(s =>
      Sub.WebSocket(
        endpoints.session.ws(s.id),
        msg =>
          Action.UpdatePlanningSession(decode[PlanningSession](msg).right.get)
      )
    )
  }
}

object PlanningPokerApp {

  case class AppState(
      user: Option[Participant],
      session: Option[CurrentPlanningSession]
  )

  case class CurrentPlanningSession(
      id: String,
      planningSession: Option[PlanningSession]
  )

  sealed trait Action
  object Action {
    case class Login(name: String) extends Action
    case class RequestSession() extends Action
    case class ReceiveSession(sessionId: String) extends Action
    case class SendPlanningSessionAction(action: PlanningSession.Action) extends Action
    case class UpdatePlanningSession(session: PlanningSession) extends Action
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
