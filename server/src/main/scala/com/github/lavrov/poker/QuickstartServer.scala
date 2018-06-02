package com.github.lavrov.poker

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object QuickstartServer extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("planningPoker")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val sessionManager: ActorRef = system.actorOf(SessionManager.props, "sessionManager")

  lazy val routes: Route = Routes

  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
}

