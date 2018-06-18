package client

import cats.effect.IO
import client.PlanningPokerApp.{Action, AppState, Store, Sub}
import com.github.lavrov.poker.Protocol
import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom.{CloseEvent, ErrorEvent, MessageEvent}
import outwatch.Sink
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scala.concurrent.duration._
import org.scalajs.dom.WebSocket
import cats.data.OptionT

object WebSocketSupport {
  val sockets: scala.collection.mutable.Map[String, OpenWebSocket] = scala.collection.mutable.Map.empty

  private def createSocket(url: String): IO[OpenWebSocket] = IO.async { cb =>
    val ws = new WebSocket(url)
    ws.onopen = { _ =>
      val ows = new OpenWebSocket(ws)
      cb(Right(ows))
    }
    ws.onerror = (e: ErrorEvent) => cb(Left(new Exception(e.message)))
  }

  private def attachPingStream(ows: OpenWebSocket): IO[Cancelable] = {
    val pings =
      Observable.timerRepeated(1.second, 1.second, Protocol.ClientMessage.Ping: Protocol.ClientMessage)
      .map(_.asJson.noSpaces)
    ows.sink <-- pings
  }

  def getOrElseCreate(url: String): IO[OpenWebSocket] =
    for {
      wsOpt <- IO { sockets.get(url) }
      ws <-
        wsOpt.fold(
          for {
            ows <- createSocket(url)
	    _ <- attachPingStream(ows)
          }
          yield {
            sockets.update(url, ows)
            ows
          }
        )(IO.pure)
    }
    yield ws

  def closeAndRemove(url: String): IO[Option[Unit]] = (
    for {
      ws <- OptionT(IO { sockets.remove(url) })
      res <- OptionT.liftF(ws.close())
    }
    yield res
  ).value

  def send(url: String, message: Protocol.ClientMessage): IO[Action] =
    for {
      ws <- getOrElseCreate(url)
    }
    yield {
      ws.sink.unsafeOnNext(message.asJson.noSpaces)
      Action.Noop
    }

  def enhance(store: Store, subscriptions: AppState => Option[Sub.WebSocket]): Store = {
    var currentSub: Option[Sub.WebSocket] = None
    val (wsSinks, sink) = Sink.redirect2[Action, Action, Observable[Action]](store.sink)((wsObs, downstreamObs) =>
      Observable.merge(downstreamObs, wsObs.merge)
    )
    val source =
      store.source.mapEval { state =>
        def close(sub: Sub.WebSocket): IO[Unit] =
          for {
            _ <- closeAndRemove(sub.url)
          }
          yield
            currentSub = None

        def connect(sub: Sub.WebSocket): IO[Unit] =
          for {
            ws <- getOrElseCreate(sub.url)
          }
          yield {
            val connectedAction = sub.connectedAction.toList
            val disconnectedAction = sub.disconnectedAction.toList
            val actionStream =
              ws.source
              .flatMap { m =>
                val serverMessage = decode[Protocol.ServerMessage](m.data.toString).right.get
                serverMessage match {
                  case Protocol.ServerMessage.Pong => Observable.empty
                  case m: Protocol.ServerMessage.SessionUpdated => Observable(m)
                }
              }
              .map(sub.actionFn)
	      .doOnErrorEval(_ => close(sub))
	      .doOnCompleteEval(close(sub))
              .startWith(connectedAction)
              .endWith(disconnectedAction)
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

final class OpenWebSocket(ws: WebSocket) {
    
  val source: Observable[MessageEvent] =
    Observable.create[MessageEvent](Unbounded) {
      observer =>
        ws.onmessage = (e: MessageEvent) => observer.onNext(e)
        ws.onerror = (e: ErrorEvent) => observer.onError(new Exception(e.message))
        ws.onclose = (e: CloseEvent) => observer.onComplete()
        Cancelable( () => ws.close() )
    }

  val sink: Sink[String] =
    Sink.create[String](
      s => IO {
        ws.send(s)
        Continue
      },
      _ => IO.pure(()),
      () => IO(ws.close())
    )

  def close(): IO[Unit] = IO { ws.close() }

}
