package io.scalac.seed.domain

import akka.actor._

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

  def props(id: String): Props = Props(new VehicleAggregateActor(id))
}

class VehicleAggregateActor(id: String) extends VehicleAggregate(id) with AggregateRootActor

class VehicleAggregate(id: String) extends AggregateRoot {

  import AggregateRoot._
  import VehicleAggregate._

  override def aggregateId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case VehicleInitialized(reg, col) =>
      _stateBehavior = created
      _state = Vehicle(id, reg, col)
    case RegNumberChanged(reg) => state match {
      case s: Vehicle => _state = s.copy(regNumber = reg)
      case _ => //nothing
    }
    case ColorChanged(col) => state match { 
      case s: Vehicle => _state = s.copy(color = col)
      case _ => //nothing
    }
    case VehicleRemoved =>
      _stateBehavior = removed
      _state = Removed
  }

  val initial: StateBehavior = {
    case Initialize(reg, col) =>
      VehicleInitialized(reg, col)
    case GetState =>
      _state
  }
  
  val created: StateBehavior = {
    case ChangeRegNumber(reg) =>
      RegNumberChanged(reg)
    case ChangeColor(color) => 
      ColorChanged(color)
    case Remove =>
      VehicleRemoved
    case GetState =>
      _state
  }
  
  val removed: StateBehavior = {
    case GetState =>
      _state
  }

  override def restore(stateToRestore: State) = {
    _state = stateToRestore
    stateToRestore match {
      case Uninitialized => _stateBehavior = initial
      case Removed => _stateBehavior = removed
      case _: Vehicle => _stateBehavior = created
    }
  }

  var _stateBehavior = initial

}