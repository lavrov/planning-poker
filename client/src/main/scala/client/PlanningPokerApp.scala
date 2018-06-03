package client

import cats.effect.IO
import com.github.lavrov.poker._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import outwatch.{Handler, Sink}
import outwatch.http.Http

class PlanningPokerApp(endpoints: Endpoints, initState: PlanningPokerApp.AppState) {
  import PlanningPokerApp._

  def createStore: IO[Store] =
    for {
      handler <- Handler.create[Action]
    }
    yield {
      val store0 = outwatch.util.Store(initState, reducer, handler)
      val store1 = WebSocketSupport.enhance(Store(store0.source, store0.sink), subscriptions)
      store1
    }

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
      state.copy(session = Some(CurrentPlanningSession(sessionId, None))) -> None
    case Action.Login(userName) =>
      state.copy(user = Some(Participant(userName, userName))) -> None
    case Action.UpdatePlanningSession(session) =>
      state.copy(session = state.session.map(_.copy(planningSession = Some(session)))) -> None
    case Action.SendPlanningSessionAction(psAction) =>
      state -> state.session.map(_.id).map { sessionId =>
        import io.circe.syntax._, io.circe.generic.auto._
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
}
