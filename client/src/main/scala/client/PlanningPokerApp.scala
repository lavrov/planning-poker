package client

import cats.effect.IO
import client.view._
import com.github.lavrov.poker._
import monix.execution.Scheduler.Implicits.global
import outwatch.{Handler, Sink}
import outwatch.dom.VNode
import outwatch.dom.dsl._
import outwatch.http.Http

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
    case class StartSession() extends Action
    case class SetSessionId(id: String) extends Action
    case class Login(name: String) extends Action
    case class UpdateSession(session: PlanningSession) extends Action
    case class SessionAction(action: PlanningSession.Action) extends Action
    case object Noop extends Action
  }

  type Store = outwatch.util.Store[AppState, Action]
  def Store(initState: AppState): IO[Store] =
    for { handler <- Handler.create[Action] }
    yield
      outwatch.util.Store(initState, reducer, handler)

  def reducer(state: AppState, action: Action): (AppState, Option[IO[Action]]) = action match {
    case Action.StartSession() =>
      state -> Some {
        val request = Http.Request("http://localhost:8080/session")
        Http.single(request, Http.Post)
          .attempt
          .map {
            case Right(response) =>
              if (response.status == 200) {
                println("Received " + response.body)
                Action.SetSessionId(response.body.toString)
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
    case Action.SetSessionId(sessionId) =>
      state.copy(session = Some(CurrentPlanningSession(sessionId, None))) -> None
    case Action.Login(userName) =>
      // TODO: request
      state.copy(user = Some(Participant(userName, userName))) -> None
    case Action.UpdateSession(session) =>
      state.copy(session = state.session.map(_.copy(planningSession = Some(session)))) -> None
    case Action.SessionAction(psAction) =>
      state.copy(
        session = state.session.map( currentSession =>
          currentSession.copy(
            planningSession = currentSession.planningSession.map(
              PlanningSession.update(_, psAction))))) -> None
    case Action.Noop =>
      state -> None
  }

  def view(store: Store): VNode = {
    def sessionJoinSink(state: AppState): Option[Sink[Unit]] =
      state.session.flatMap(_.planningSession).zip(state.user).headOption // if user and session are defined
        .collect {
          case (session, user) if !session.participants.contains(user) =>
            store.redirectMap(_ =>
              PlanningPokerApp.Action.SessionAction(
                PlanningSession.Action.AddParticipant(user)))
        }
    div(
      children <-- store.map(state =>
        List(
          UserView.render(
            state.user,
            sessionJoinSink(state),
            store.redirectMap(PlanningPokerApp.Action.Login)
          ),
          state.session match {
            case Some(session) =>
              session.planningSession match {
                case Some(planningSession) =>
                  PlanningSessionView.render(planningSession, state.user, store)
                case None =>
                  div("Connecting...")
              }
            case None =>
              button(
                "Start session",
                onClick(Action.StartSession()) --> store
              )
          },
          state.session match {
            case Some(session) => div("Session id: ", session.id)
            case None => div()
          }
        )
      )
    )
  }
}
