package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.{Action, AppState}
import com.github.lavrov.poker.{Participant, PlanningSession}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object AppView {

  def render(state: AppState, sink: Sink[Action]): VNode = {
    state.user match {
      case None =>
        SignInView.render(sink.redirectMap(PlanningPokerApp.Action.Login))
      case Some(user) =>
        div(
          state.session match {
            case Some(session) =>
              session.planningSession match {
                case Some(planningSession) =>
                  PlanningSessionView.render(planningSession, user, sink)
                case None =>
                  div("Connecting...")
              }
            case None =>
              div(className := "text-center",
                button(classNames := Seq("btn", "btn-lg btn-primary"), "Start session",
                  onClick(Action.RequestSession()) --> sink))
          }
        )
    }
  }
}
