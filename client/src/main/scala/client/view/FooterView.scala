package client.view

import outwatch.dom.VNode
import outwatch.dom.dsl._

object FooterView {
  def render(): VNode =
    footer(`class` := "footer text-center",
      div(`class` := "container",
        span(`class` := "text-muted", "by "),
        a(href := "https://lunatech.com", "Lunatech")
      )
    )
}
