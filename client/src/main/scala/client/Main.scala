package client

import client.PlanningPokerApp.Store
import com.github.lavrov.poker._
import monix.execution.Scheduler.Implicits.global
import outwatch.dom.OutWatch

object Main {
  def main(args: Array[String]): Unit = {

    val initState = PlanningSession(
      List(
        Participant("0", "Vitaly"),
        Participant("1", "Ikenna"),
        Participant("2", "Natalya"),
        Participant("3", "Ender")
      ),
      Estimates(
        UserStory("Story description here"),
        Map.empty
      )
    )

    val run =
      for {
        store <- Store(initState)
        root = PlanningPokerApp.view(store)
        _ <- OutWatch.renderInto("#app", root)
      } yield ()

    run.unsafeRunSync()
  }
}
