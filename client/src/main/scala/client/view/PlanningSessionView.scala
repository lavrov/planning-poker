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
      planningSession.players
        .map(_.id)
        .forall(planningSession.estimates.participantEstimates.contains)
    def isParticipating(user: Participant) = planningSession.players.contains(user)
    div(
      h1(planningSession.estimates.userStory.description),
      hr(),
      userOpt.filter(isParticipating).fold(div()){
        user => CardsView.render(
          planningSession.estimates.participantEstimates.get(user.id),
          store.redirectMap { card =>
            PlanningPokerApp.Action.SendPlanningSessionAction(
              PlanningSession.Action.RegisterEstimate(user.id, card))
          }
        )
      },
      div(
        "Players",
        ul(
          planningSession.players.map { participant =>
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
            li(name, " ", status)
          }
        ),
        "Observers",
        ul(
          for (u <- planningSession.observers)
          yield
            li(u.name)
        )
      )
    )
  }
}
