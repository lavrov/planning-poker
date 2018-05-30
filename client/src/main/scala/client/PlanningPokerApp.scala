package client

import cats.effect.IO
import client.view.PlanningSessionView
import com.github.lavrov.poker.{Action, PlanningSession}
import monix.execution.Scheduler
import outwatch.Handler
import outwatch.dom.VNode
import outwatch.dom.dsl._

object PlanningPokerApp {
  type Store = outwatch.util.Store[PlanningSession, Action]
  def Store(initState: PlanningSession)(implicit scheduler: Scheduler): IO[Store] =
    for { handler <- Handler.create[Action] }
    yield
      outwatch.util.Store(initState, reducer, handler)

  def reducer(state: PlanningSession, action: Action): (PlanningSession, Option[IO[Action]]) = {
    (PlanningSession.update(state, action), None)
  }

  def view(store: Store): VNode =
    div(child <-- store.map(PlanningSessionView.render(_, store)))
}
