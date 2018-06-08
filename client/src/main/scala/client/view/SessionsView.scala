package client.view

import client.PlanningPokerApp.{Action, CurrentPlanningSession}
import client.{Page, Routing}
import com.github.lavrov.poker.{Participant, PlanningSession}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object SessionsView {

  def render(user: Participant, activeSession: Option[CurrentPlanningSession], sink: Sink[Action]): VNode = {
    activeSession match {
      case Some(session) =>
        a(className := "btn btn-primary", href := Routing.hashPath(Page.Session(session.id)),
          "Open active session")
      case _ =>
        div(className := "text-center",
          button(classNames := Seq("btn", "btn-lg btn-primary"),
            "Start new session",
            onClick(Action.RequestSession()) --> sink))
    }
  }

}
