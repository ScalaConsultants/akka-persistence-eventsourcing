package io.scalac.seed.domain

import io.scalac.seed.domain.AggregateRoot.{Event, StateBehavior, State}

object AggregateRoot {

  trait State

  case object Uninitialized extends State

  case object Removed extends State

  trait Event

  trait Command

  case object Remove extends Command

  case object GetState extends Command

  type StateBehavior = PartialFunction[Command, Any]

}

abstract class AggregateRoot(val aggregateId: String, val state: State) {

  def updateState(event: Event): State

  val stateBehavior: StateBehavior
}
