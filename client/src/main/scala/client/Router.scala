package client

import client.PlanningPokerApp.Action
import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom.window

object Router {
  type HashString = String

  def create(): Observable[Action]  = {
    println(s"Hash ${window.location.hash}")
    Observable.create[HashString](Unbounded) { subscruber =>
      subscruber.onNext(window.location.hash)
      window.onhashchange = event => subscruber.onNext(window.location.hash)
      Cancelable(() => window.onhashchange = null)
    }
      .map(routes)
  }

  val routes: HashString => Action = {
    val Session = """#/session/(.+)""".r
    ({
      case Session(id) => Action.ReceiveSession(id)
      case _ => Action.Noop
    })
  }
}
