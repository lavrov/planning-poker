package com.github.lavrov.poker

import akka.actor.{ Actor, ActorLogging, Props }


object SessionActor {
  final case class AddParticipant(participant: Participant)
  final case class ClearEstimates()
  case class Vote(participant: Participant, card: Card)
  def props: Props = Props[SessionActor]
}

//TODO Send BroadCasts and reply

class SessionActor( val id: UserStory) extends Actor with ActorLogging {
  import SessionActor._

  var participantEstimates: Map[String, Card] = Map()
  var participants: List[Participant] = List[Participant]()

  def receive: Receive = {
    case AddParticipant(participant) =>
      participants :+ participant
    case ClearEstimates =>
      participantEstimates = Map()
    case Vote(participant, card) =>
      participantEstimates += (participant.id -> card)

  }
}