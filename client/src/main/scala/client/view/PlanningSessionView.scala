package client.view

import client.PlanningPokerApp.Store
import com.github.lavrov.poker.{Action, Card, PlanningSession}
import outwatch.dom.VNode
import outwatch.dom.dsl._

object PlanningSessionView {
  def render(planningSession: PlanningSession, store: Store): VNode = {
    val allGaveEstimates =
      planningSession.participants
        .map(_.id)
        .forall(planningSession.estimates.participantEstimates.contains)
    div(
      h1(planningSession.estimates.userStory.description),
      hr(),
      ul(
        planningSession.participants.map { participant =>
          val name = participant.name
          val status =
            planningSession.estimates.participantEstimates.get(participant.id)
              .map {
                case Card.Number(n) =>
                  if (allGaveEstimates)
                    n.toString
                  else
                    "+"
              }
              .getOrElse("-")
          li(name,
            button(status,
              onClick(Action.RegisterEstimate(participant.id, Card.Number(5))) --> store)
          )
        }
      )
    )
  }
}
