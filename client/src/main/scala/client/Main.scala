package client

import client.PlanningPokerApp.{AppState, Store}
import monix.execution.Scheduler.Implicits.global
import outwatch.dom.OutWatch

object Main {
  def main(args: Array[String]): Unit = {

    val initState = AppState(None, None)

    val run =
      for {
        store <- Store(initState)
        root = PlanningPokerApp.view(store)
        _ <- OutWatch.renderInto("#app", root)
      } yield ()

    run.unsafeRunSync()
  }
}
