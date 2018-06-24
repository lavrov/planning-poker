package client

import cats.effect.IO
import client.PlanningPokerApp.{Action, AppState, Store, Sub}
import com.github.lavrov.poker.Protocol
import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom._
import outwatch.Sink
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

import scala.concurrent.duration._
import cats.data.OptionT

object WebSocketSupport {
  val sockets: scala.collection.mutable.Map[String, OpenWebSocket] = scala.collection.mutable.Map.empty

  private def attachPingStream(ows: OpenWebSocket): IO[Unit] = {
    val pings =
      Observable.timerRepeated(1.second, 1.second, Protocol.ClientMessage.Ping: Protocol.ClientMessage)
      .map(_.asJson.noSpaces)
    (ows.sink <-- pings).map { sub =>
      ows.source.doOnTerminate(_ => sub.cancel())
      ()
    }
  }

  def getOrElseCreate(url: String): IO[OpenWebSocket] =
    for {
      wsOpt <- IO { sockets.get(url) }
      ws <-
        wsOpt.fold(
          for {
            ows <- OpenWebSocket.create(url)
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
      _ <- OptionT.liftF(ws.sink <-- Observable.empty)
    }
    yield ()
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
        def close(sub: Sub.WebSocket): IO[Option[Action]] =
          for {
            _ <- closeAndRemove(sub.url)
          }
          yield {
            currentSub = None
            sub.disconnectedAction
          }

        def connect(sub: Sub.WebSocket): IO[Option[Action]] =
          getOrElseCreate(sub.url)
          .attempt
          .map {
            case Right(ws) =>
	      val actionStream =
		ws.source
		.timeoutOnSlowUpstream(5.seconds)
		.flatMap { m =>
		  val serverMessage = decode[Protocol.ServerMessage](m.data.toString).right.get
		  serverMessage match {
		    case Protocol.ServerMessage.Pong => Observable.empty
		    case m: Protocol.ServerMessage.SessionUpdated => Observable(m)
		  }
		}
		.map(sub.actionFn)
		.onErrorFallbackTo(Observable.empty)
		.++(Observable.fromIO(close(sub)).collect { case Some(a) => a })
	      wsSinks.unsafeOnNext(actionStream)
	      currentSub = Some(sub)
              sub.connectedAction
            case Left(error) =>
              sub.disconnectedAction
          }

        val io =
          (currentSub, subscriptions(state)) match {
            case (None, None) => IO.pure(None)
            case (Some(s), None) =>
              close(s)
            case (None, Some(s)) =>
              connect(s)
            case (Some(current), Some(updated)) =>
              if (current.url == updated.url) IO.pure(None) // the same subscrbtion -- do nothing
              else
                close(current).flatMap(_ => connect(updated))
          }
        for (maybeAction <- io)
        yield {
          maybeAction.foreach(sink.unsafeOnNext)
          state
        }
      }
    Store(source, sink)
  }
}

final case class OpenWebSocket(source: Observable[MessageEvent], sink: Sink[String])

object OpenWebSocket {

  private def readyWS(url: String): IO[WebSocket] = IO.async { cb =>
    val ws = new WebSocket(url)
    ws.onopen = { _ =>
      cb(Right(ws))
    }
    ws.onerror = (_: Event) => cb(Left(new Exception("Cannot create WebSocket")))
  }

  def create(url: String): IO[OpenWebSocket] =
    for {
      ws <- readyWS(url)
      source =
      Observable.create[MessageEvent](Unbounded) {
        observer =>
          ws.onmessage = (e: MessageEvent) => observer.onNext(e)
          ws.onerror = (_: Event) => observer.onError(new Exception("WebSocket error"))
          ws.onclose = (_: CloseEvent) => observer.onComplete()
          Cancelable( () => ws.close() )
      }
      sink <-
        Sink.create[String](
          next = s => {
            ws.send(s)
            Continue
          },
          complete = () => ws.close()
        )
    }
    yield {
      new OpenWebSocket(source, sink)
    }
}
