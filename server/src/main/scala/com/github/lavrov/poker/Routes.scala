package com.github.lavrov.poker

import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

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
import com.github.lavrov.poker.Protocol.ClientMessage

class Routes(
    sessionManager: ActorRef
)(
    implicit
      system: ActorSystem,
      materializer: ActorMaterializer
) {
  import system.dispatcher
  import CirceSupport._

  lazy val log = Logging(system, classOf[Routes])

  private implicit lazy val timeout = Timeout(30.seconds)

  def route: Route =
    respondWithHeader(`Access-Control-Allow-Origin`.*) {
      pathPrefix("session") {
        post {
          complete(
            (sessionManager ? SessionManager.Create()).mapTo[String]
          )
        } ~
        path(Segment / "ws" / Segment) { case (sessionId, userId) =>
          extractUpgradeToWebSocket { upgrade =>
            val response =
              (sessionManager ? SessionManager.Get(sessionId))
                .mapTo[Option[ActorRef]]
                .map {
                  case Some(sessionActorRef) =>
                    val (ref: ActorRef, sessionActorSource: Source[TextMessage.Strict, _]) =
                      Source
                        .actorRef[PlanningSession](100, OverflowStrategy.dropNew)
                        .map(ps => Protocol.ServerMessage.SessionUpdated(ps): Protocol.ServerMessage)
                        .map(m => TextMessage.Strict(m.asJson.noSpaces))
                        .preMaterialize()

                    sessionActorRef ! SessionActor.Subscribe(userId, ref)

                    val clientMessages: Flow[Message, ClientMessage, _] = Flow.fromFunction {
                      case TextMessage.Strict(tm) =>
                        decode[Protocol.ClientMessage](tm) match {
                          case Right(clientMessage) =>
                            clientMessage
                          case Left(error) =>
                            log.error(error, error.getMessage)
                            throw error
                        }
                      case _ =>
                        throw new Exception("Message not supported")
                    }

                    val handleClientFlow: Flow[Message, Message, _] = clientMessages.flatMapMerge(
                      breadth = 1,
                      f = {
                        case Protocol.ClientMessage.Ping =>
                          val pong: Protocol.ServerMessage = Protocol.ServerMessage.Pong
                          Source.single(TextMessage.Strict(pong.asJson.noSpaces))
                        case Protocol.ClientMessage.SessionAction(action) =>
                          sessionActorRef ! SessionActor.SessionAction(action)
                          Source.empty
                      }
                    )

                    val handleFlow = handleClientFlow.merge(sessionActorSource)
                    upgrade.handleMessages(handleFlow)
                  case None =>
                    HttpResponse(StatusCodes.NotFound)
                }
            complete(response)
          }
        }
      }
    }
}

