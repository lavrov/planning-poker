package com.github.lavrov.poker


 case class PlanningSession(
    participants: Map[String, Participant],
    players: Set[String],
    estimates: Estimates
)

case class Participant(
    id: String,
    name: String
)

case class UserStory(
    description: String
)

case class Estimates(
    storyText: String = "",
    participantEstimates: Map[String, Card] = Map.empty
)

sealed trait Card
object Card {
  case class Number(value: Int) extends Card
  case object Dunno extends Card
}


object PlanningSession {
  sealed trait Action
  object Action {
    case class AddObserver(participant: Participant) extends Action
    case class AddPlayer(participant: Participant) extends Action
    case class RemoveParticipant(id: String) extends Action
    case class RegisterEstimate(participantId: String, card: Option[Card]) extends Action
    case object ClearEstimates extends Action
    case class SetStoryText(text: String) extends Action
  }
  import Action._

  def update(model: PlanningSession, action: Action): PlanningSession = action match {
    case AddObserver(participant) =>
      model.copy(
        participants = model.participants + (participant.id ->participant),
        players = model.players - participant.id
      )
    case AddPlayer(participant) =>
      model.copy(
        participants = model.participants + (participant.id ->participant),
        players = model.players + participant.id
      )
    case RemoveParticipant(id) =>
      model.copy(
        participants = model.participants - id
      )
    case RegisterEstimate(participantId, card) =>
      if (model.players.contains(participantId))
        model.copy(
          estimates = model.estimates.copy(
            participantEstimates =
              card match {
                case Some(c) =>
                  model.estimates.participantEstimates.updated(participantId, c)
                case None =>
                  model.estimates.participantEstimates - participantId
              }))
      else
        model
    case ClearEstimates =>
      model.copy(estimates = Estimates())
    case SetStoryText(text) =>
      model.copy(estimates = model.estimates.copy(storyText = text))
  }
}