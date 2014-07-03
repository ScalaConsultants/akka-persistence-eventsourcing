package io.scalac.seed.domain

import akka.actor._
import akka.persistence._

object VehicleAggregate {

  import AggregateRoot._

  case class Vehicle(id: String, regNumber: String = "", color: String = "") extends State

  case class Initialize(regNumber: String, color: String) extends Command
  case class ChangeRegNumber(newRegNumber: String) extends Command
  case class ChangeColor(newColor: String) extends Command

  case class VehicleInitialized(regNumber: String, color: String) extends Event
  case class RegNumberChanged(regNumber: String) extends Event
  case class ColorChanged(color: String) extends Event
  case object VehicleRemoved extends Event

  def props(id: String): Props = Props(new VehicleAggregate(id))
}

class VehicleAggregate(id: String) extends AggregateRoot {

  import AggregateRoot._
  import VehicleAggregate._

  override def persistenceId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case VehicleInitialized(reg, col) =>
      context.become(created)
      state = Vehicle(id, reg, col)
    case RegNumberChanged(reg) => state match {
      case s: Vehicle => state = s.copy(regNumber = reg) 
      case _ => //nothing
    }
    case ColorChanged(col) => state match { 
      case s: Vehicle => state = s.copy(color = col)
      case _ => //nothing
    }
    case VehicleRemoved =>
      context.become(removed)
      state = Removed
  }

  val initial: Receive = {
    case Initialize(reg, col) =>
      persist(VehicleInitialized(reg, col))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }
  
  val created: Receive = {
    case ChangeRegNumber(reg) =>
      persist(RegNumberChanged(reg))(afterEventPersisted)
    case ChangeColor(color) => 
      persist(ColorChanged(color))(afterEventPersisted)
    case Remove =>
      persist(VehicleRemoved)(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }
  
  val removed: Receive = {
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val receiveCommand: Receive = initial

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    this.state = state
    state match {
      case Uninitialized => context become initial
      case Removed => context become removed
      case _: Vehicle => context become created
    }
  }

}