package com.github.lavrov.poker

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

trait Routes  {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def sessionActor: ActorRef

  implicit lazy val timeout = Timeout(30.seconds)

  lazy val Routes: Route =
    pathPrefix("session") {
      concat(
        pathEnd {
          concat(
            get {
              val users: Future[PlanningSession] = ???
              complete(???)
            }/*,
            post {
              entity(as[PlanningSession]) { session =>
                val sessionCreated: Future[ActionPerformed] = ???
                complete((StatusCodes.Created))
              }
            }*/
          )
        }
      )
    }
}
