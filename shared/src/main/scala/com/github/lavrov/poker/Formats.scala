package com.github.lavrov.poker

import io.circe._, io.circe.generic.semiauto._, io.circe.syntax._, io.circe.parser._

object Formats {
  implicit val cardEncoder: Encoder[Card] = deriveEncoder
  implicit val estimatesEncoder: Encoder[Estimates] = deriveEncoder
  implicit val participantEncoder: Encoder[Participant] = deriveEncoder
  implicit val planningSessionEncoder: Encoder[PlanningSession] = deriveEncoder
  implicit val planningSessionActionEncoder: Encoder[PlanningSession.Action] = deriveEncoder
  implicit val serverProtocolEncoder: Encoder[Protocol.ServerMessage] = deriveEncoder
  implicit val clientProtocolEncoder: Encoder[Protocol.ClientMessage] = deriveEncoder

  implicit val cardDecoder: Decoder[Card] = deriveDecoder
  implicit val estimatesDecoder: Decoder[Estimates] = deriveDecoder
  implicit val participantDecoder: Decoder[Participant] = deriveDecoder
  implicit val planningSessionDecoder: Decoder[PlanningSession] = deriveDecoder
  implicit val planningSessionActionDecoder: Decoder[PlanningSession.Action] = deriveDecoder
  implicit val serverProtocolDecoder: Decoder[Protocol.ServerMessage] = deriveDecoder
  implicit val clientProtocolDecoder: Decoder[Protocol.ClientMessage] = deriveDecoder
}
