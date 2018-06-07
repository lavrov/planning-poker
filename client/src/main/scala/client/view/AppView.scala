package client.view

import client.{Endpoints, Page, PlanningPokerApp, Routing}
import client.PlanningPokerApp.{Action, AppState}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object AppView {

  def render(state: AppState, sink: Sink[Action], endpoints: Endpoints): VNode = {
    div(
      HeaderView.render(state.user.map(_.name), sink),
      tag("main")(className := "container",
        state.page match {
          case Page.Home =>
              p("Welcome to Planning Poker")
          case Page.SignIn(_) =>
            SignInView.render(sink.redirectMap(PlanningPokerApp.Action.SignIn))
          case Page.Session(_, _) =>
            state.session.zip(state.user).headOption match {
              case Some((session, user)) =>
                session.planningSession match {
                  case Some(planningSession) =>
                    PlanningSessionView.render(planningSession, user, sink)
                  case None =>
                    div("Connecting...")
                }
              case None =>
                div("invalid state")
            }
          case Page.Sessions(_) =>
            state.session match {
              case Some(session) =>
                a(className := "btn btn-primary", href := Routing.hashPath(Page.Session(session.id)),
                  "Open active session")
              case _ =>
                div(className := "text-center",
                  button(classNames := Seq("btn", "btn-lg btn-primary"),
                    "Start new session",
                    onClick(Action.RequestSession()) --> sink))
            }
          case _ =>
            div("invalid route")
        }
      ),
      FooterView.render()
    )
  }
}
