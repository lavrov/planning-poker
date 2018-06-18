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
            state.user.fold(p("Welcome to Planning Poker")) { u =>
              SessionsView.render(u, None, sink)
            }
          case Page.SignIn(_) =>
            SignInView.render(sink.redirectMap(PlanningPokerApp.Action.SignIn))
          case Page.Session(_, _) =>
            state.user.fold(signInBtn) { user =>
              state.session match {
                case Some(sessionEither) =>
                  sessionEither match {
                    case Right(currentSession) =>
                      PlanningSessionView.render(currentSession, user, sink)
                    case Left(reason) =>
                      div(reason)
                  }
                case None =>
                  div("Connecting...")
              }
            }
          case Page.Sessions(_) =>
            state.user.fold(signInBtn) { u =>
              SessionsView.render(u, state.session, sink)
            }
          case _ =>
            div("invalid route")
        }
      ),
      FooterView.render()
    )
  }

  private def signInBtn =
    a(
      "Sign in",
      className := "btn btn-primary",
      href := Routing.hashPath(Page.SignIn())
    )
}
