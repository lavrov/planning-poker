package client

import org.scalajs.dom.ext.{LocalStorage => JSLocalStorage}
import client.PlanningPokerApp.AppState
import com.github.lavrov.poker.Participant
import io.circe.parser._, io.circe.syntax._, io.circe.generic.auto._

object LocalStorage {
  def initialState = AppState(
      Routing.Home,
      JSLocalStorage("app.user").flatMap(value =>
        decode[Participant](value).right.toOption
      ),
      None
    )
  def persist(user: Participant): Unit = {
    JSLocalStorage.update("app.user", user.asJson.noSpaces)
  }
}
