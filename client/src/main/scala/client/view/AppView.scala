package client.view

import client.{Endpoints, PlanningPokerApp}
import client.PlanningPokerApp.{Action, AppState}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object AppView {

  def render(state: AppState, sink: Sink[Action], endpoints: Endpoints): VNode = {
    state.user match {
      case None =>
        SignInView.render(sink.redirectMap(PlanningPokerApp.Action.Login))
      case Some(user) =>
        div(
          state.session match {
            case Some(session) =>
              session.planningSession match {
                case Some(planningSession) =>
                  List(
                    PlanningSessionView.render(planningSession, user, sink),
                    a(href := endpoints.router.session(session.id), "Share")
                  )
                case None =>
                  div("Connecting...")
              }
            case None =>
              div(className := "text-center",
                button(classNames := Seq("btn", "btn-lg btn-primary"), "Start session",
                  onClick(Action.RequestSession()) --> sink))
          },
        )
    }
  }
}
