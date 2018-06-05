package client

import cats.effect.IO
import client.PlanningPokerApp.{Action, AppState, Store, Sub}
import outwatch.util.WebSocket
import monix.execution.Scheduler.Implicits.global

object WebSocketSupport {
  val sockets: scala.collection.mutable.Map[String, WebSocket] = scala.collection.mutable.Map.empty

  def getOrElseCreate(url: String): IO[WebSocket] = IO {
    sockets.getOrElseUpdate(url, WebSocket(url))
  }
  def closeAndRemove(url: String): IO[Unit] = IO {
    sockets.get(url).foreach { ws =>
      ws.ws.close()
      sockets.remove(url)
    }
  }
  def send(url: String, message: String): IO[Action] =
    getOrElseCreate(url).map { ws =>
      println(s"Send $message")
      ws.ws.send(message)
      Action.Noop
    }

  def enhance(store: Store, subscriptions: AppState => Option[Sub.WebSocket]): Store = {
    var currentSub: Option[Sub.WebSocket] = None
    val source =
      store.source.mapEval { state =>
        def close(sub: Sub.WebSocket): IO[Unit] = for {
          _ <- closeAndRemove(sub.url)
        }
          yield
            currentSub = None

        def connect(sub: Sub.WebSocket): IO[Unit] = for {
          ws <- getOrElseCreate(sub.url)
          _ <- store.sink <-- ws.source.map { m => sub.actionFn(m.data.toString) }
        }
          yield
            currentSub = Some(sub)

        val io =
          (currentSub, subscriptions(state)) match {
            case (None, None) => IO.pure(())
            case (Some(s), None) =>
              close(s)
            case (None, Some(s)) =>
              println(s"Subscribe $s")
              connect(s)
            case (Some(current), Some(updated)) =>
              if (current.url == updated.url) IO.pure(()) // the same subscrbtion -- do nothing
              else
                close(current).flatMap(_ => connect(updated))
          }
        for (_ <- io) yield state
      }
    Store(source, store.sink)
  }
}
