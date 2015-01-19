package io.scalac.seed.domain

import io.scalac.seed.domain.AggregateRoot._

object AggregateRoot {

  trait State

  case object Uninitialized extends State

  case object Removed extends State

  trait Event

  trait Command

  case object Remove extends Command

  case object GetState extends Command

}

trait AggregateRoot {

  def calculateState(id: String, state: State, event: Event): State

  def calculateEvents(id: String, state: State, command: Command): Event

  def executeQuery(id: String, state:State, query: Command): State

}

class DefaultAggregateRootAdapter extends AggregateRootAdapter[State, Command, Event] {
  this: AggregateRoot ⇒

  def acceptCommand(id: String, state: State, afterEventReaction: (State, Event) ⇒ Unit, queryReaction: (State) ⇒ Unit): PartialFunction[Command, Unit] =
  {
    case GetState ⇒ queryReaction(executeQuery(id, state, GetState))
    case command: Command ⇒ 
      val events = calculateEvents(id, state, command)
      afterEventReaction(calculateState(id, state, events), events)
  }

  def recover(id: String, state: State, event: Event): State = calculateState(id, state, event)
}
