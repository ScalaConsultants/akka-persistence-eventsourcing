package io.scalac.seed.vehicle.route

import akka.actor._
import java.util.UUID
import io.scalac.seed.vehicle.domain.VehicleAggregate
import io.scalac.seed.common.RestMessage

case class RegisterVehicle(regNumber: String, color: String, ownerId: String) extends RestMessage
case class GetVehicle(id: String) extends RestMessage
case class GetVehicles() extends RestMessage
case class UpdateRegNumber(id: String, regNumber: String) extends RestMessage

object VehicleAggregateManager {
  def props: Props = Props(new VehicleAggregateManager)
}

class VehicleAggregateManager extends Actor {

  import VehicleAggregateManager._
  import VehicleAggregate._
  
  var aggregates = Map[String, ActorRef]()
  
  def receive = {
    case RegisterVehicle(rn, col, oid) =>
      val id = UUID.randomUUID().toString()
      val agg = create(id)
      agg ! SetState(rn, col, oid)
      agg forward GetState()
    case GetVehicle(id) =>
      val aggregate = findOrCreate(id) 
      aggregate forward GetState()
    case UpdateRegNumber(id, regNumber) =>
      val aggregate = findOrCreate(id)
      aggregate ! ChangeRegNumber(regNumber)
      aggregate forward GetState()
  }
  
  def create(id: String): ActorRef = {
    val agg = context.actorOf(VehicleAggregate.props(id))
    aggregates += id -> agg
    agg
  }
  
  def findOrCreate(id: String): ActorRef = 
    aggregates.get(id) getOrElse { create(id) }
  
}