package com.github.lavrov.poker

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object QuickstartServer extends App {
  implicit val system: ActorSystem = ActorSystem("planningPoker")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val sessionManager: ActorRef = system.actorOf(SessionManager.props, "sessionManager")

  val routes: Routes = new Routes(sessionManager)

  val port = sys.env.get("PORT").map(_.toInt).getOrElse(8080)

  Http().bindAndHandle(routes.route, "0.0.0.0", port)

  println(s"Server online at 0.0.0.0:$port")

  Await.result(system.whenTerminated, Duration.Inf)
}

