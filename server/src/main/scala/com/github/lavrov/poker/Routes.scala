package com.github.lavrov.poker

import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import java.util.UUID

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Route
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import akka.pattern.ask
import com.github.lavrov.poker.SessionManager.{IncomingMessage, RequestSession, Subscribe}

trait Routes {
import CirceSupport._
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  lazy val log = Logging(system, classOf[Routes])
  
  def sessionManager: ActorRef

  implicit lazy val timeout = Timeout(30.seconds)

  lazy val Routes: Route =
    respondWithHeader(`Access-Control-Allow-Origin`.*) {
      pathPrefix("session" / "ws" / Segment) { sessionId =>

        val (ref: ActorRef, source: Source[TextMessage.Strict, _]) =
          Source
            .actorRef[PlanningSession](100, OverflowStrategy.dropNew)
            .map(m => TextMessage.Strict(m.asJson.noSpaces))
            .preMaterialize()
        sessionManager ! Subscribe(sessionId, ref)

        val sink = Sink
          .actorRef[IncomingMessage](sessionManager, ()).
          contramap[Message] {
          case TextMessage.Strict(tm) =>
            decode[PlanningSession.Action](tm) match {
              case Right(action) =>
                log.info(s"Received action $action")
                IncomingMessage(sessionId, action)
              case Left(error) =>
                log.error(error, error.getMessage())
                throw error
            }
        }

        handleWebSocketMessages(Flow.fromSinkAndSource(sink, source))
      } ~
      path("session") {
        post {
          val id = UUID.randomUUID().toString
          complete(
            (sessionManager ? RequestSession(id)).mapTo[Done].map(_ => id)(system.dispatcher)
          )
        }
      }
    }
}

