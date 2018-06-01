package com.github.lavrov.poker

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import io.circe.generic.auto._, io.circe.syntax._, io.circe.parser._

class PlanningSessionSpec extends WordSpec {
  classOf[PlanningSession].getName should {
    "be serialized with circe" in {
      val instance = PlanningSession(
        Nil,
        Estimates(UserStory("desc"), Map.empty)
      )
      val json = instance.asJson
      val str = json.noSpaces
      str shouldEqual """{"participants":[],"estimates":{"userStory":{"description":"desc"},"participantEstimates":{}}}"""
      decode[PlanningSession](str) shouldEqual Right(instance)
    }
  }
}
