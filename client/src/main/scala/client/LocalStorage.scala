package client

import cats.effect.IO
import org.scalajs.dom.ext.{LocalStorage => JSLocalStorage}
import client.PlanningPokerApp.AppState
import com.github.lavrov.poker.Participant
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._

object LocalStorage {
  def initialState = AppState(
      Routing.Home,
      JSLocalStorage("app.user").flatMap(value =>
        decode[Participant](value).right.toOption
      )
    )
  def persist(user: Participant): IO[Unit] = IO {
    JSLocalStorage.update("app.user", user.asJson.noSpaces)
  }
}
