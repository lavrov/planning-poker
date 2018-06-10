package com.github.lavrov.poker

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}


object SessionActor {
  final case class Subscribe(userId: String, ref: ActorRef)
  final case class SessionAction(action: PlanningSession.Action)
  case object Get
  def props: Props = Props[SessionActor](new SessionActor)
}

class SessionActor extends Actor with ActorLogging {
  import SessionActor._

  var subscribers: Map[ActorRef, String] = Map.empty
  var planningSession: PlanningSession = PlanningSession(
    Map.empty,
    Set.empty,
    Estimates()
  )

  def receive: Receive = {
    case Subscribe(userId: String, ref) =>
      log.info(s"New subscriber $userId")
      subscribers += (ref -> userId)
      context.watch(ref)
      ref ! planningSession
    case SessionAction(action) =>
      val updated = PlanningSession.update(planningSession, action)
      planningSession = updated
      subscribers.keys.foreach(_ ! planningSession)
    case Get =>
      sender() ! planningSession
    case Terminated(ref) =>
      val userId = subscribers(ref)
      log.info(s"Unsubscribe $userId $ref")
      planningSession = PlanningSession.update(planningSession, PlanningSession.Action.RemoveParticipant(userId))
      subscribers -= ref
  }
}