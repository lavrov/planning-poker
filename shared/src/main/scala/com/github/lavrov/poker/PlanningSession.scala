package com.github.lavrov.poker


 case class PlanningSession(
    participants: List[Participant],
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
    userStory: UserStory,
    participantEstimates: Map[String, Card]
)

sealed trait Card
object Card {
  case class Number(value: Int) extends Card
  case object Dunno extends Card
}


object PlanningSession {
  sealed trait Action
  object Action {
    case class AddParticipant(participant: Participant) extends Action
    case class RegisterEstimate(participantId: String, card: Option[Card]) extends Action
  }
  import Action._

  def update(model: PlanningSession, action: Action): PlanningSession = action match {
    case AddParticipant(participant) =>
      model.copy(
        participants = participant :: model.participants)
    case RegisterEstimate(participantId, card) =>
      if (model.participants.exists(_.id == participantId))
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
  }
}