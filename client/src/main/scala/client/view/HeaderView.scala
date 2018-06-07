package client.view

import client.Routing
import outwatch.dom.VNode
import outwatch.dom.dsl._

object HeaderView {
  def render(user: Option[String]): VNode =
    header(className := " p-3 px-md-4 mb-3 bg-white border-bottom box-shadow",
      div(className := "container d-flex flex-column flex-md-row align-items-center",
        h5(className := "my-0 mr-md-auto font-weight-normal", "Planning Poker"),
        user match {
          case Some(u) =>
            div(u)
          case _ =>
            a(className := "btn btn-outline-primary", "Sign in", href := Routing.hashPath(Routing.SignIn()))
        }
      )
    )

}
