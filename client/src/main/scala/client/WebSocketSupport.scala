package client

import cats.effect.IO
import client.PlanningPokerApp.{Action, AppState, Store, Sub}
import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom.{CloseEvent, ErrorEvent, MessageEvent}
import outwatch.Sink

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
    for {
      ws <- getOrElseCreate(url)
    }
    yield {
      ws.sink.unsafeOnNext(message)
      Action.Noop
    }

  def enhance(store: Store, subscriptions: AppState => Option[Sub.WebSocket]): Store = {
    var currentSub: Option[Sub.WebSocket] = None
    val (wsSinks, sink) = Sink.redirect2[Action, Action, Observable[Action]](store.sink)((wsObs, downstreamObs) =>
      Observable.merge(downstreamObs, wsObs.merge)
    )
    val source =
      store.source.mapEval { state =>
        def close(sub: Sub.WebSocket): IO[Unit] = for {
          _ <- closeAndRemove(sub.url)
        }
          yield
            currentSub = None

        def connect(sub: Sub.WebSocket): IO[Unit] = for {
          ws <- getOrElseCreate(sub.url)
        }
        yield {
          val actionStream = ws.source.map(m => sub.actionFn(m.data.toString))
          wsSinks.unsafeOnNext(actionStream)
          currentSub = Some(sub)
        }

        val io =
          (currentSub, subscriptions(state)) match {
            case (None, None) => IO.pure(())
            case (Some(s), None) =>
              close(s)
            case (None, Some(s)) =>
              connect(s)
            case (Some(current), Some(updated)) =>
              if (current.url == updated.url) IO.pure(()) // the same subscrbtion -- do nothing
              else
                close(current).flatMap(_ => connect(updated))
          }
        for (_ <- io) yield state
      }
    Store(source, sink)
  }
}

final case class WebSocket(url: String) {
  val ws = new org.scalajs.dom.WebSocket(url)

  val source: Observable[MessageEvent] = {
    var buffer = List.empty[MessageEvent]
    ws.onmessage = { (e: MessageEvent) =>
      buffer = e :: buffer
    }
    Observable.create[MessageEvent](Unbounded)(observer => {
      observer.onNextAll(buffer.reverse)
      buffer = null
      ws.onmessage = (e: MessageEvent) => observer.onNext(e)
      ws.onerror = (e: ErrorEvent) => observer.onError(new Exception(e.message))
      ws.onclose = (e: CloseEvent) => observer.onComplete()
      Cancelable( () => ws.close() )
    })
  }

  val sink: Sink[String] = {
    var buffer = List.empty[String]
    val result =
      Sink.create[String](
        s => IO {
          if (ws.readyState == org.scalajs.dom.WebSocket.OPEN)
            ws.send(s)
          else
            buffer = s :: buffer
          Continue
        },
        _ => IO.pure(()),
        () => IO(ws.close())
      )
    ws.onopen = { _ =>
      buffer.reverse.foreach(result.unsafeOnNext)
      buffer = null
    }
    result
  }

}
