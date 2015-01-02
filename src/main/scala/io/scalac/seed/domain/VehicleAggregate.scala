package io.scalac.seed.domain

import io.scalac.seed.domain.AggregateRoot._
import io.scalac.seed.domain.VehicleAggregate._

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

}

object VehicleAggregateProvider extends AggregateRootProvider{
  def aggregateRoot(id: String, state: State): AggregateRoot = state match {
    case Uninitialized ⇒ new VehicleAggregateInitial(id, state)
    case Removed ⇒ new VehicleAggregateRemoved(id, state)
    case _: Vehicle ⇒ new VehicleAggregateCreated(id, state)
  }
}

abstract class VehicleAggregate(aggregateId: String, state: State) extends AggregateRoot(aggregateId, state) {
  def updateState(event: Event): State = event match {
    case VehicleInitialized(reg, col) =>
      Vehicle(aggregateId, reg, col)
    case RegNumberChanged(reg) => state match {
      case s: Vehicle => s.copy(regNumber = reg)
      case _ => state
    }
    case ColorChanged(col) => state match {
      case s: Vehicle => s.copy(color = col)
      case _ => state
    }
    case VehicleRemoved =>
      Removed
  }
}

class VehicleAggregateInitial(aggregateId: String, state: State) extends VehicleAggregate(aggregateId, state) {
  val stateBehavior: StateBehavior = {
    case Initialize(reg, col) =>
      VehicleInitialized(reg, col)
    case GetState =>
      state
  }
}

class VehicleAggregateCreated(aggregateId: String, state: State) extends VehicleAggregate(aggregateId, state) {
  val stateBehavior: StateBehavior = {
    case ChangeRegNumber(reg) =>
      RegNumberChanged(reg)
    case ChangeColor(color) =>
      ColorChanged(color)
    case Remove =>
      VehicleRemoved
    case GetState =>
      state
  }
}

class VehicleAggregateRemoved(aggregateId: String, state: State) extends VehicleAggregate(aggregateId, state) {
  val stateBehavior: StateBehavior = {
    case GetState =>
      state
  }
}
