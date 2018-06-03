package com.github.lavrov.poker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.lavrov.poker.SessionActor.SessionAction

object SessionManager {
  final case class RequestSession(id: String)
  final case class Subscribe(id: String, ref: ActorRef)
  final case class IncomingMessage(id: String, action: PlanningSession.Action)

  def props: Props = Props[SessionManager](new SessionManager())
}

class SessionManager extends Actor with ActorLogging {
  import SessionManager._
  var sessionIdToActor = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case RequestSession(id) =>
      sessionIdToActor.get(id) match {
        case Some(sessionRef) => sessionRef forward SessionAction
        case None =>
          val sessionRef = context.actorOf(SessionActor.props, id)
          sessionIdToActor += (id -> sessionRef)
          sessionRef forward SessionAction
      }
    case Subscribe(id: String, ref: ActorRef) =>
      sessionIdToActor.get(id) match {
        case Some(sessionRef) => sessionRef forward SessionActor.Subscribe(ref)
        case None             =>
      }
    case IncomingMessage(id, action: PlanningSession.Action) =>
      sessionIdToActor.get(id) match {
        case Some(sessionRef) => sessionRef forward action
        case None             =>
      }

  }

}
