package com.github.lavrov.poker

import akka.actor.{ Actor, ActorLogging, Props }


object SessionActor {
  def props: Props = Props[SessionActor]
}

class SessionActor extends Actor with ActorLogging {
  def receive: Receive = {
    case _ => ???
  }
}