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
        user.fold(
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
        )( u =>
          div(u.name,
            onSessionJoin.fold(
              div()
            )(
              onJoin =>
                button(
                  "join",
                  onClick(()) --> onJoin
                ))))
    }
    yield vNode
}
