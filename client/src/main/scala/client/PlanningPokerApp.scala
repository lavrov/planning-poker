package client

import cats.effect.IO
import client.view._
import com.github.lavrov.poker._
import monix.execution.Scheduler
import outwatch.Handler
import outwatch.dom.VNode
import outwatch.dom.dsl._

object PlanningPokerApp {
  case class AppState(
      user: Option[Participant],
      session: Option[PlanningSession]
  )

  sealed trait Action
  object Action {
    case class StartSession() extends Action
    case class Login(name: String) extends Action
    case class UpdateSession(session: PlanningSession) extends Action
    case class SessionAction(action: PlanningSession.Action) extends Action
  }

  type Store = outwatch.util.Store[AppState, Action]
  def Store(initState: AppState)(implicit scheduler: Scheduler): IO[Store] =
    for { handler <- Handler.create[Action] }
    yield
      outwatch.util.Store(initState, reducer, handler)

  def reducer(state: AppState, action: Action): (AppState, Option[IO[Action]]) = action match {
    case Action.StartSession() =>
      // TODO: request
      val dummySession = PlanningSession(
        List(
          Participant("0", "Vitaly"),
          Participant("1", "Ikenna"),
          Participant("2", "Natalya"),
          Participant("3", "Ender")
        ),
        Estimates(
          UserStory("Story description here"),
          Map.empty
        )
      )
      state.copy(session = Some(dummySession)) -> None
    case Action.Login(userName) =>
      // TODO: request
      state.copy(user = Some(Participant(userName, userName))) -> None
    case Action.UpdateSession(session) =>
      state.copy(session = Some(session)) -> None
    case Action.SessionAction(psAction) =>
      state.copy(
        session = state.session.map(PlanningSession.update(_, psAction))) -> None
  }

  def view(store: Store): VNode =
    div(
      children <-- store.map( state =>
        List(
          UserView.render(
            state.user,
            state.session.zip(state.user).headOption.collect {
              case (session, user) if !session.participants.contains(user) =>
              store.redirectMap(_ =>
                PlanningPokerApp.Action.SessionAction(
                  PlanningSession.Action.AddParticipant(user)))
            },
            store.redirectMap(PlanningPokerApp.Action.Login)),
          state.session.fold(
            button(
              "Start session",
              onClick(Action.StartSession()) --> store
            )
          )(
            session => PlanningSessionView.render(session, state.user, store)
          )
        )
      )
    )
}
