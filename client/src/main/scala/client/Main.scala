package client

import client.view.AppView
import monix.execution.Scheduler.Implicits.global
import outwatch.dom.OutWatch
import outwatch.dom.dsl._

object Main {
  def main(args: Array[String]): Unit = {
    JsImport.bootstrap
    JsImport.animate
    JsImport.main
    val initState = LocalStorage.initialState
    val endpoints = new Endpoints("planning-poker-server.herokuapp.com", secure = true)
    val app = new PlanningPokerApp(endpoints, initState)

    val run =
      for {
        store <- app.createStore
        root = div(child <-- store.source.map(AppView.render(_, store.sink, endpoints)))
        _ <- OutWatch.renderInto("#app", root)
      } yield ()

    run.unsafeRunSync()
  }
}
