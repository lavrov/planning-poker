package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.{Action, Store}
import com.github.lavrov.poker.{Card, Participant, PlanningSession}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object PlanningSessionView {
  def render(planningSession: PlanningSession, userOpt: Option[Participant], store: Sink[Action]): VNode = {
    val allGaveEstimates =
      planningSession.participants
        .map(_.id)
        .forall(planningSession.estimates.participantEstimates.contains)
    div(
      h1(planningSession.estimates.userStory.description),
      hr(),
      userOpt.fold(div()){
        user => CardsView.render(
          None,
          store.redirectMap { card =>
            PlanningPokerApp.Action.SessionAction(
              PlanningSession.Action.RegisterEstimate(user.id, card))
          }
        )
      },
      ul(
        planningSession.participants.map { participant =>
          val name = participant.name
          val status =
            planningSession.estimates.participantEstimates.get(participant.id)
              .map { card =>
                if (allGaveEstimates)
                  CardsView.cardSign(card)
                else
                  "+"
              }
              .getOrElse("-")
          li(name, status)
        }
      )
    )
  }
}
