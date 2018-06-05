package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.Action
import com.github.lavrov.poker.{Participant, PlanningSession}
import outwatch.Sink
import outwatch.dom.VNode
import outwatch.dom.dsl._

object PlanningSessionView {
  def render(planningSession: PlanningSession, user: Participant, sink: Sink[Action]): VNode = {
    val allGaveEstimates =
      planningSession.players
        .map(_.id)
        .forall(planningSession.estimates.participantEstimates.contains)
    def isPlayer = planningSession.players.contains(user)
    def becomePlayer =
        PlanningPokerApp.Action.SendPlanningSessionAction(
          PlanningSession.Action.AddPlayer(user))
    def becomeObserver =
      PlanningPokerApp.Action.SendPlanningSessionAction(
        PlanningSession.Action.AddObserver(user))
    div(
      h1(planningSession.estimates.userStory.description),
      hr(),
      div(
        div(
          className := "btn-group btn-group-toggle",
          role := "group",
          button(
            classNames := "btn" :: (if (isPlayer) List("btn-primary", "active") else List("btn-secondary")),
            "Player",
            onClick(becomePlayer) --> sink
          ),
          button(
            classNames := "btn" :: (if (!isPlayer) List("btn-primary", "active") else List("btn-secondary")),
            "Observer",
            onClick(becomeObserver) --> sink
          )
        ),
        if (isPlayer)
          Some(
            CardsView.render(
              planningSession.estimates.participantEstimates.get(user.id),
              sink.redirectMap { card =>
                PlanningPokerApp.Action.SendPlanningSessionAction(
                  PlanningSession.Action.RegisterEstimate(user.id, card))
              }))
        else
          None
      ),
      div(
        "Players",
        ul(
          planningSession.players.toList.map { participant =>
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
          for (u <- planningSession.observers.toList)
          yield
            li(u.name)
        )
      )
    )
  }
}
