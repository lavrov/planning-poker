package client.view

import client.PlanningPokerApp
import client.PlanningPokerApp.Action
import client.PlanningPokerApp.CurrentPlanningSession
import com.github.lavrov.poker.{Card, Participant, PlanningSession}
import outwatch.{Handler, Sink}
import outwatch.dom.VNode
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import mouse.boolean._

object PlanningSessionView {
  def render(currentSession: CurrentPlanningSession, user: Participant, sink: Sink[Action]): VNode = {
    import currentSession.planningSession
    val (players, observers) = planningSession.participants.values.toList.partition(p => planningSession.players(p.id))
    val allGaveEstimates =
      players.forall(p => planningSession.estimates.participantEstimates.contains(p.id))
    val estimates =
      players
        .flatMap(p => planningSession.estimates.participantEstimates.get(p.id))
        .collect {
          case Card.Number(v) => v
        }
    def mean = estimates.sum / estimates.size
    def isPlayer = planningSession.players.contains(user.id)
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
      header(isPlayer, allGaveEstimates, planningSession.estimates.storyText, currentSession.status,
        becomePlayer, becomeObserver, clearEstimates, setStoryText),
      div(
        isPlayer.option(
          CardsView.render(
            planningSession.estimates.participantEstimates.get(user.id),
            sink.redirectMap { card =>
              PlanningPokerApp.Action.SendPlanningSessionAction(
                PlanningSession.Action.RegisterEstimate(user.id, card))
            }))
      ),
      div(className := "row",
        div(className := "col-sm",
          div(className := "card my-3 box-shadow",
            div(className := "card-header", "Players"),
            ul(className := "list-group list-group-flush",
              players.map { participant =>
                  val name = participant.name
                  val status =
                    planningSession.estimates.participantEstimates.get(participant.id)
                      .map { card => if (allGaveEstimates) CardsView.cardSign(card) else "+" }
                  li(className := "list-group-item d-flex justify-content-between align-items-center",
                    name,
                    for (st <- status) yield
                      span(className := "badge badge-primary badge-pill animated bounceIn", st)
                  )
              }
            )
          ),
          observers.nonEmpty.option(
            div(className := "card my-3 box-shadow",
              div(className := "card-header", "Observers"),
              ul(className := "list-group list-group-flush",
                for (u <- observers)
                yield
                  li(className := "list-group-item d-flex justify-content-between align-items-center",
                    u.name
                  )
              )
            )
          )
        ),
        div(className := "col-sm",
          allGaveEstimates.option(
            div(className := "card my-3 box-shadow",
              div(className := "card-header", "Stats"),
              div(className := "card-body",
                span("Average: ", mean)
              )
            )
          )
        )
      )
    )
  }

  private def header(
      isPlayer: Boolean,
      estimationOver: Boolean,
      text: String,
      connected: Boolean,
      becomePlayer: Sink[Unit], becomeObserver: Sink[Unit], nextRound: Sink[Unit], storyText: Sink[String]): VNode = {
    for {
      storyText$ <- Handler.create[String]
      vNode <- {
        val isObserver = !isPlayer
        def btnClasses(isActive: Boolean) =
          "btn btn-sm" :: isActive.fold(List("btn-secondary", "active"), List("btn-outline-secondary"))
        def nextRoundBtnClass = estimationOver.fold("btn-primary", "btn-outline-secondary")

        div(
          div(`class` := "d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2",
            div(`class` := "btn-toolbar my-2",
              div(`class` := "btn-group mr-2",
                button(classNames := "btn" :: "btn-sm" :: nextRoundBtnClass :: Nil,
                  onClick(()) --> nextRound, "Next round")
              ),
              div(`class` := "btn-group mr-2",
                button(classNames := btnClasses(isPlayer), onClick(()) --> becomePlayer, "Player"),
                button(classNames := btnClasses(isObserver), onClick(()) --> becomeObserver, "Observer")
              )
            ),
            span(`class` := s"badge badge-pill badge-${if (connected) "success" else "danger"} m-2",
              if (connected) "Connected" else "Disconnected"
            )
          ),
          form(
            className := "form-inline border-bottom",
            onSubmit.map(e => e.preventDefault())(storyText$) --> storyText,
            input(`type` := "text", `class` := "form-control form-control-lg border-0",
              placeholder := "Enter story description",
              value := text,
              onInput.value --> storyText$
            )
          )
        )
      }
    }
    yield vNode
  }
}
