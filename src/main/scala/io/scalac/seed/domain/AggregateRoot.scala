package io.scalac.seed.domain

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

trait AggregateRoot {

  import AggregateRoot._

  def aggregateId: String

  def state :State = _state

  def stateBehavior :StateBehavior = _stateBehavior

  protected var _state: State = Uninitialized

  protected var _stateBehavior: StateBehavior

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  def updateState(evt: Event): Unit

  def restore(stateToRestore: State): Unit

}
