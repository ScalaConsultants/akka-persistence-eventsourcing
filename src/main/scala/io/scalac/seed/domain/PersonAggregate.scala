package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import io.scalac.seed.domain.AggregateRoot.{Uninitialized, Remove, GetState, Removed}

object PersonAggregate {

  import AggregateRoot._

  case class Person(id: String, name: String = "") extends State

  case class Initialize(name: String) extends Command

  case class PersonInitialized(name: String) extends Event
  case object PersonRemoved extends Event

  def props(id: String): Props = Props(new PersonAggregate(id))
}

class PersonAggregate(id: String) extends AggregateRoot {

  import PersonAggregate._

  override def processorId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case PersonInitialized(name) =>
      context.become(created)
      state = Person(id, name)
    case PersonRemoved =>
      context.become(removed)
      state = Removed
  }

  val initial: Receive = {
    case Initialize(name) =>
      persist(PersonInitialized(name))(afterEventPersisted)
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
  }

  val created: Receive = {
    case Remove =>
      persist(PersonRemoved)(afterEventPersisted)
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
  }

  val removed: Receive = {
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
  }

  val receiveCommand: Receive = initial

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: AggregateRoot.State) = {
    this.state = state
    state match {
      case Uninitialized => context become initial
      case Removed => context become removed
      case _: Person => context become created
    }
  }

}