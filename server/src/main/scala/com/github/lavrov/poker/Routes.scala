package com.github.lavrov.poker

import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, RootActorPath}
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.Await
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.github.lavrov.poker.SessionActor.Subscribe

trait Routes {

  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  lazy val log = Logging(system, classOf[Routes])

  def sessionActor: ActorRef


  implicit lazy val timeout = Timeout(30.seconds)

  lazy val Routes: Route =
    respondWithHeader(`Access-Control-Allow-Origin`.*) {
      pathPrefix("session" / Segment) { sessionId =>
        // TODO
        val sessionActorRef: ActorRef = Await.result(
          system.actorSelection(s"/user/$sessionId").resolveOne(1.second), 2.seconds)
        val (ref: ActorRef, source: Source[Message, _]) =
          Source
            .actorRef[Message](100, OverflowStrategy.dropNew)
            .preMaterialize()
        sessionActorRef ! Subscribe(ref)

        val sink: Sink[Message, NotUsed] =
          Sink.actorRef[PlanningSession.Action](sessionActorRef, ())
            .contramap[Message] {
            case TextMessage.Strict(tm) =>
              log.info(tm)
              decode[PlanningSession.Action](tm) match {
                case Right(action) =>
                  log.info(s"Received action $action")
                  action
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
          val ref = system.actorOf(SessionActor.props, id)
          log.info(ref.path.toString)
          complete(id)
        }
      }
    }
}
