package io.scalac.seed.service

import akka.actor._
import io.scalac.seed.domain._
import java.util.UUID

object VehicleAggregateManager {

  import AggregateManager._

  case class RegisterVehicle(regNumber: String, color: String) extends Command
  case class GetVehicle(id: String) extends Command
  case class UpdateRegNumber(id: String, regNumber: String) extends Command
  case class UpdateColor(id: String, color: String) extends Command
  case class DeleteVehicle(id: String) extends Command
  
  def props: Props = Props(new VehicleAggregateManager)
}

class VehicleAggregateManager extends AggregateManager {

  import AggregateRoot._
  import VehicleAggregateManager._
  import VehicleAggregate._

  def processCommand = {
    case RegisterVehicle(rn, col) =>
      val id = UUID.randomUUID().toString()
      processAggregateCommand(id, Initialize(rn, col))
    case GetVehicle(id) =>
      processAggregateCommand(id, GetState)
    case UpdateRegNumber(id, regNumber) =>
      processAggregateCommand(id, ChangeRegNumber(regNumber))
    case UpdateColor(id, color) =>
      processAggregateCommand(id, ChangeColor(color))
    case DeleteVehicle(id) =>
      processAggregateCommand(id, Remove)
  }

  override def aggregateProps(id: String) = VehicleAggregate.props(id)
}