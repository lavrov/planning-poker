package client

import cats.effect.IO
import cats.instances.string._
import client.PlanningPokerApp.{Action, Store}
import client.PlanningPokerApp.Action.ChangePage
import org.scalajs.dom.window
import outwatch.dom.dsl.events
import com.github.werk.router4s.Router
import monix.reactive.Observable

object Routing {

  private val paths = {
    val path = new Router[Page]
    val str = Router.Node[String](Some(_), Some(_), "str")

    path(Page.Home,
      path("signin", Page.SignIn),
      path("sessions", Page.Sessions,
        path(str, Page.Session)
      )
    )
  }

  def enhance(store: Store): IO[Store]  = IO {
    val locationChanges =
      events.window.onHashChange
        .map(_ => window.location.hash)
        .startWith(window.location.hash :: Nil)
        .map(parse(_).map(ChangePage).getOrElse(ChangePage(Page.Home)))
    store.copy(sink = store.sink.redirect[Action](original => Observable.merge(original, locationChanges)))
  }

  def parse(hashString: String): Option[Page] = {
    println(s"Parse $hashString")
    val result = paths.data(if (hashString.headOption.contains('?')) hashString drop 2 else hashString.drop(1))
    println(s"Parsed to $result")
    result
  }

  def hashPath(page: Page): String = "#" + paths.path(page)

  def navigate(page: Page): IO[Action]= IO {
    window.location.hash = hashPath(page)
    Action.Noop
  }
}

sealed trait Page
object Page {
  sealed trait SignedInUser

  case object Home extends Page
  case class SignIn(parent: Home.type = Home) extends Page
  case class Sessions(parent: Home.type = Home) extends Page
  case class Session(id: String, parent: Sessions = Sessions()) extends Page with SignedInUser
}
