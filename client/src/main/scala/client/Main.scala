package client

import client.PlanningPokerApp.{AppState, Store}
import monix.execution.Scheduler.Implicits.global
import outwatch.dom.OutWatch
import outwatch.dom.dsl._

object Main {
  def main(args: Array[String]): Unit = {

    val initState = AppState(None, None)

    val run =
      for {
        store <- Store(initState)
        (source, sink) = store
        root = div(child <-- source.map(PlanningPokerApp.view(_, sink)))
        _ <- OutWatch.renderInto("#app", root)
      } yield ()

    run.unsafeRunSync()
  }
}
