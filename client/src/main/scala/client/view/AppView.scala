package client.view

import client.{Endpoints, PlanningPokerApp, Routing}
import client.PlanningPokerApp.{Action, AppState}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object AppView {

  def render(state: AppState, sink: Sink[Action], endpoints: Endpoints): VNode = {
    div(
      HeaderView.render(),
      tag("main")(className := "container",
        state.page match {
          case Routing.Home =>
            state.user match {
              case None =>
                SignInView.render(sink.redirectMap(PlanningPokerApp.Action.Login))
              case Some(_) =>
                state.session match {
                  case Some(session) =>
                    a(className := "btn btn-primary", href := Routing.hashPath(Routing.Session(session.id)),
                      "Open active session")
                  case _ =>
                    div(className := "text-center",
                      button(classNames := Seq("btn", "btn-lg btn-primary"),
                        "Start session",
                        onClick(Action.RequestSession()) --> sink))
                }
            }
          case Routing.Session(_, _) =>
            state.session.zip(state.user).headOption match {
              case Some((session, user)) =>
                session.planningSession match {
                  case Some(planningSession) =>
                    div(
                      PlanningSessionView.render(planningSession, user, sink),
                      a(href := Routing.hashPath(Routing.Session(session.id)), "Share")
                    )
                  case None =>
                    div("Connecting...")
                }
              case None =>
                div("invalid state")
            }
          case _ =>
            div("invalid route")
        }
      )
    )
  }
}
