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

  trait Accepted

  case class AcceptedEvent(event: Event, newState: State) extends Accepted

  case class AcceptedQuery(state: Any) extends Accepted

  type StateBehavior = PartialFunction[Command, Any]

}

abstract class AggregateRoot(val aggregateId: String, val state: State) {

  def updateState(event: Event): State

  protected val stateBehavior: StateBehavior

  def acceptEvent:PartialFunction[Command, Accepted] = {
    case x:Command if stateBehavior.isDefinedAt(x) ⇒ {
       stateBehavior.apply(x) match {
         case event:Event ⇒ AcceptedEvent(event, updateState(event))
         case state ⇒ AcceptedQuery(state)
       }
    }
  }

}
