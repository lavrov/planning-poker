package com.github.lavrov.poker

object Protocol {
  sealed trait ClientMessage
  object ClientMessage {
    case object Ping extends ClientMessage
    case class SessionAction(payload: PlanningSession.Action) extends ClientMessage
  }
  sealed trait ServerMessage
  object ServerMessage {
    case object Pong extends ServerMessage
    case class SessionUpdated(payload: PlanningSession) extends ServerMessage
  }
}
