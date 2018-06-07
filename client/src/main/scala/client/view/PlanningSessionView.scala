package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.Action
import com.github.lavrov.poker.{Participant, PlanningSession}
import outwatch.{Handler, Sink}
import outwatch.dom.VNode
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

object PlanningSessionView {
  def render(planningSession: PlanningSession, user: Participant, sink: Sink[Action]): VNode = {
    val allGaveEstimates =
      planningSession.players
        .map(_.id)
        .forall(planningSession.estimates.participantEstimates.contains)
    def isPlayer = planningSession.players.contains(user)
    def becomePlayer = sink.redirectMap[Unit](_ =>
        PlanningPokerApp.Action.SendPlanningSessionAction(
          PlanningSession.Action.AddPlayer(user)))
    def becomeObserver = sink.redirectMap[Unit](_ =>
      PlanningPokerApp.Action.SendPlanningSessionAction(
        PlanningSession.Action.AddObserver(user)))
    def clearEstimates = sink.redirectMap[Unit](_ =>
      PlanningPokerApp.Action.SendPlanningSessionAction(
        PlanningSession.Action.ClearEstimates))
    def setStoryText = sink.redirectMap[String](text =>
      PlanningPokerApp.Action.SendPlanningSessionAction(
        PlanningSession.Action.SetStoryText(text)))
    div(
      header(isPlayer, allGaveEstimates, planningSession.estimates.storyText,
        becomePlayer, becomeObserver, clearEstimates, setStoryText),
      div(
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
      div(className := "row",
        div(className := "col-sm",
          div(className := "card my-3 box-shadow",
            div(className := "card-header", "Players"),
            ul(className := "list-group list-group-flush",
              planningSession.players.toList.map { participant =>
                val name = participant.name
                val status =
                  planningSession.estimates.participantEstimates.get(participant.id)
                    .map { card => if (allGaveEstimates) CardsView.cardSign(card) else "+" }
                li(className := "list-group-item d-flex justify-content-between align-items-center",
                  name,
                  for (st <- status) yield
                    span(className := "badge badge-primary badge-pill", st)
                )
              }
            )
          ),
          if (planningSession.observers.nonEmpty)
            div(className := "card my-3 box-shadow",
              div(className := "card-header", "Observers"),
              ul(className := "list-group list-group-flush",
                for (u <- planningSession.observers.toList)
                yield
                  li(className := "list-group-item d-flex justify-content-between align-items-center",
                    u.name
                  )
              )
            )
          else
            div()
        ),
        div(className := "col-sm",
          div(className := "card my-3 box-shadow",
            div(className := "card-header", "Stats")
          )
        )
      )
    )
  }

  private def header(
      isPlayer: Boolean,
      estimationOver: Boolean,
      text: String,
      becomePlayer: Sink[Unit], becomeObserver: Sink[Unit], nextRound: Sink[Unit], storyText: Sink[String]): VNode = {
    for {
      storyText$ <- Handler.create[String]
      vNode <- {
        val isObserver = !isPlayer
        def btnClasses(isActive: Boolean) =
          "btn btn-sm" :: List(if (isActive) "btn-secondary" else "btn-outline-secondary")
        div(`class` := "d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom",
          form(className := "form-inline", onSubmit(storyText$) --> storyText,
            input(
              `type` := "text",
              `class` := "form-control form-control-lg border-0",
              placeholder := "Enter story description",
              value := text,
              onInput.value --> storyText$
            )),
          div(`class` := "btn-toolbar my-2",
            div(`class` := "btn-group mr-2",
              button(`class` := "btn btn-sm btn-secondary", onClick(()) --> nextRound, "Next round")
            ),
            div(`class` := "btn-group mr-2",
              button(classNames := btnClasses(isPlayer), onClick(()) --> becomePlayer, "Player"),
              button(classNames := btnClasses(isObserver), onClick(()) --> becomeObserver, "Observer")
            )
          )
        )
      }
    }
    yield vNode
  }
}
