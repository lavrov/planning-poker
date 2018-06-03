package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.{Action, AppState}
import com.github.lavrov.poker.PlanningSession
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object AppView {

  def render(state: AppState, sink: Sink[Action]): VNode = {
    def sessionJoinSink(state: AppState): Option[Sink[Unit]] =
      state.session.flatMap(_.planningSession).zip(state.user).headOption // if user and session are defined
        .collect {
        case (session, user) if !session.participants.contains(user) =>
          sink.redirectMap(_ =>
            PlanningPokerApp.Action.SendPlanningSessionAction(
              PlanningSession.Action.AddParticipant(user)))
      }
    div(
      UserView.render(
        state.user,
        sessionJoinSink(state),
        sink.redirectMap(PlanningPokerApp.Action.Login)
      ),
      state.session match {
        case Some(session) =>
          session.planningSession match {
            case Some(planningSession) =>
              PlanningSessionView.render(planningSession, state.user, sink)
            case None =>
              div("Connecting...")
          }
        case None =>
          button(
            "Start session",
            onClick(Action.RequestSession()) --> sink
          )
      },
      state.session match {
        case Some(session) => div("Session id: ", session.id)
        case None => div()
      }
    )
  }
}
