package client.view

import com.github.lavrov.poker.Card
import outwatch.dom.VNode
import outwatch.Sink
import outwatch.dom.dsl._

object CardsView {
  val configuration = List(
    Card.Number(1),
    Card.Number(2),
    Card.Number(3),
    Card.Number(5),
    Card.Number(8),
    Card.Number(13),
    Card.Number(20),
    Card.Dunno
  )

  def render(selected: Option[Card], onSelect: Sink[Option[Card]]): VNode =
    div(className := "my-3",
      className := "btn-group btn-group-toggle",
      role := "group",
      configuration.map(card =>
        renderCard(
          card,
          selected.contains(card),
          onSelect
        )
      )
    )

  private def renderCard(card: Card, selected: Boolean, onSelect1: Sink[Option[Card]]) = {
    val text = cardSign(card)
    val cls = List("btn", "btn-lg")
    val btnClass = if (selected) "btn-primary" else "btn-outline-secondary"
    button(
      classNames := btnClass :: cls,
      text,
      onClick(if (selected) None else Some(card)) --> onSelect1
    )
  }

  def cardSign(card: Card): String = card match {
    case Card.Number(n) => n.toString
    case Card.Dunno => "?"
  }

}
