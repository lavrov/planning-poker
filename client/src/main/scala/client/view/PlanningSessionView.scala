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
    def sessionJoinSink(user: Participant): Option[Sink[Unit]] = {
      if (!isPlayer) Some {
        sink.redirectMap(_ =>
          PlanningPokerApp.Action.SendPlanningSessionAction(
            PlanningSession.Action.AddPlayer(user)))
      }
      else None
    }
    div(
      h1(planningSession.estimates.userStory.description),
      hr(),
      div(
        sessionJoinSink(user).map( joinSink =>
          button(className := "btn btn-sm btn-primary", "Join", onClick(()) --> joinSink)),
        CardsView.render(
          planningSession.estimates.participantEstimates.get(user.id),
          sink.redirectMap { card =>
            PlanningPokerApp.Action.SendPlanningSessionAction(
              PlanningSession.Action.RegisterEstimate(user.id, card))
          }
        )
      ),
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
