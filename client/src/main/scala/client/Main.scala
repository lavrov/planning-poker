package client

import client.PlanningPokerApp.AppState
import client.view.AppView
import monix.execution.Scheduler.Implicits.global
import outwatch.dom.OutWatch
import outwatch.dom.dsl._

object Main {
  def main(args: Array[String]): Unit = {
    BootstrapCSS
    val initState = AppState(None, None)
    val endpoints = new Endpoints("localhost:8080")
    val app = new PlanningPokerApp(endpoints, initState)

    val run =
      for {
        store <- app.createStore
        root = div(child <-- store.source.map(AppView.render(_, store.sink)))
        _ <- OutWatch.renderInto("#app", root)
      } yield ()

    run.unsafeRunSync()
  }
}
