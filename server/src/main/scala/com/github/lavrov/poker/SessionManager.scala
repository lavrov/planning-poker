package com.github.lavrov.poker

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}

object SessionManager {
  final case class Create()
  final case class Get(id: String)

  def props: Props = Props[SessionManager](new SessionManager())
}

class SessionManager extends Actor with ActorLogging {
  import SessionManager._

  override def receive: Receive = {
    case Create() =>
      val id = UUID.randomUUID.toString
      context.actorOf(SessionActor.props, id)
      sender() ! id
    case Get(id) =>
      sender() ! context.child(id)
  }
}
