package client.view

import outwatch.{Handler, Sink}
import outwatch.dom.VNode
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

object SignInView {

  def render(sink: Sink[String]): VNode = for {
    name$ <- Handler.create[String]
    vNode <-
      form(
        classNames := Seq("form-signin", "text-center"),
        h1(classNames := Seq("h3", "mb-3", "font-weight-normal"), "Please sign in"),
        label(className := "sr-only", `for` := "inputName", "Your name"),
        input(className := "form-control", `type` := "text", id := "inputName", placeholder := "Your name",
          required, autoFocus, minLength := 1,
          onInput.value --> name$
        ),
        div(className := "mb-3"),
        button(classNames := Seq("btn", "btn-lg btn-primary", "btn-block"), "Sign in",
          onClick(name$) --> sink)
      )
  }
    yield vNode

}
