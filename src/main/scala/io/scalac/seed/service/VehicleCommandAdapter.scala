package io.scalac.seed.service

import io.scalac.seed.domain.AggregateRoot.{Remove, GetState}
import io.scalac.seed.domain._
import java.util.UUID

import io.scalac.seed.service.CommandAdapter.Command

object VehicleCommandAdapter {

  case class RegisterVehicle(regNumber: String, color: String) extends Command
  case class GetVehicle(id: String) extends Command
  case class UpdateRegNumber(id: String, regNumber: String) extends Command
  case class UpdateColor(id: String, color: String) extends Command
  case class DeleteVehicle(id: String) extends Command

}

class VehicleCommandAdapter extends CommandAdapter {

  import VehicleCommandAdapter._
  import VehicleAggregate._

  def adapt(command: Command) : (String, AggregateRoot.Command) = command match {
    case RegisterVehicle(rn, col) =>
      val id = UUID.randomUUID().toString()
      (id, Initialize(rn, col))
    case GetVehicle(id) => (id, GetState)
    case UpdateRegNumber(id, regNumber) => (id, ChangeRegNumber(regNumber))
    case UpdateColor(id, color) => (id, ChangeColor(color))
    case DeleteVehicle(id) => (id, Remove)
  }

}