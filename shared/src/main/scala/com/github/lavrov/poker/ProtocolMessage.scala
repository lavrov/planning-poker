package com.github.lavrov.poker

sealed trait ProtocolMessage
object ProtocolMessage {
  case object Ping extends ProtocolMessage
  case object Pong extends ProtocolMessage
  case class ClientMessage(payload: PlanningSession.Action) extends ProtocolMessage
  case class ServerMessage(payload: PlanningSession) extends ProtocolMessage
}
