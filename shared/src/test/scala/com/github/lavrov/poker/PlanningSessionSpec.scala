package com.github.lavrov.poker

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import io.circe.syntax._, io.circe.parser._
import Formats._

class PlanningSessionSpec extends WordSpec {
  classOf[PlanningSession].getName should {
    "be serialized with circe" in {
      val instance = PlanningSession(
        Map.empty,
        Set.empty,
        Estimates("desc", Map.empty)
      )
      val json = instance.asJson
      val str = json.noSpaces
      str shouldEqual """{"participants":{},"players":[],"estimates":{"storyText":"desc","participantEstimates":{}}}"""
      decode[PlanningSession](str) shouldEqual Right(instance)
    }
  }
}
