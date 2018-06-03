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
    div(
      configuration.map(card =>
        renderCard(
          card,
          selected.contains(card),
          onSelect
        )
      ),
      button("clear", onClick(None) --> onSelect)
    )

  private def renderCard(card: Card, selected: Boolean, onSelect: Sink[Option[Card]]) = {
    val text = cardSign(card)
    val cls = if (selected) Some("selected") else None
    button(
      if (selected) strong(text) else text,
      classNames := cls,
      onClick(if (selected) None else Some(card)) --> onSelect
    )
  }

  def cardSign(card: Card): String = card match {
    case Card.Number(n) => n.toString
    case Card.Dunno => "?"
  }

}
