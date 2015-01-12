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

trait VehicleAggregateRoot extends AggregateRoot {

  def calculateState(id: String, state: State, event: Event): State = (state, event) match {
    case (Uninitialized, VehicleInitialized(reg, col)) ⇒ Vehicle(id, reg, col)
    case (s: Vehicle, RegNumberChanged(newReg)) ⇒ s.copy(regNumber = newReg)
    case (s: Vehicle, ColorChanged(newColor)) ⇒ s.copy(color = newColor)
    case (_, RegNumberChanged(_) | ColorChanged(_)) ⇒ state
    case (_, VehicleRemoved) ⇒ Removed
  }

  def executeQuery(id: String, state: State, query: Command): State = state

  def calculateEvents(id: String, state: State, command: Command): Event = (state, command) match {
    case (Uninitialized, Initialize(reg, col)) ⇒ VehicleInitialized(reg, col)
    case (_:Vehicle, ChangeRegNumber(newReg)) ⇒ RegNumberChanged(newReg)
    case (_:Vehicle, ChangeColor(color)) ⇒ ColorChanged(color)
    case (_:Vehicle, Remove) ⇒ VehicleRemoved
  }
}
