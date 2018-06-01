package com.github.lavrov.poker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}


object SessionActor {
  final case class Subscribe(ref: ActorRef)
  final case class SessionAction(action: PlanningSession.Action)
  def props: Props = Props[SessionActor](new SessionActor)
}

//TODO Send BroadCasts and reply

class SessionActor extends Actor with ActorLogging {
  import SessionActor._

  var subscribers: List[ActorRef] = Nil
  var planningSession: PlanningSession = PlanningSession(
    Nil,
    Estimates(
      UserStory("blah"),
      Map.empty
    )
  )

  def receive: Receive = {
    case Subscribe(ref) => subscribers = ref :: subscribers
    case SessionAction(action) =>
      val updated = PlanningSession.update(planningSession, action)
      planningSession = updated
      subscribers.foreach(_ ! planningSession)
  }
}