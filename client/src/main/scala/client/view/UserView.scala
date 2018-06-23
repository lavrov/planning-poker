package client.view

import com.github.lavrov.poker.Participant
import outwatch.{Handler, Sink}
import outwatch.dom.VNode
import outwatch.dom.dsl._

import monix.execution.Scheduler.Implicits.global

object UserView {
  def render(user: Option[Participant], onSessionJoin: Option[Sink[Unit]], onLogin: Sink[String]): VNode =
    for {
      name$ <- Handler.create[String]
      vNode <-
        user match {
          case Some(u) =>
            div(u.name,
              onSessionJoin.map(
                onJoin =>
                  button(
                    "join",
                    onClick(()) --> onJoin
                  )
              )
            )
          case None =>
            div(
              input(
                placeholder := "Name",
                onInput.value --> name$
              ),
              button(
                "Log in",
                onClick(name$) --> onLogin,
                disabled <-- name$.map(_.isEmpty).startWith(true :: Nil)
              )
            )
        }
    }
    yield vNode
}
