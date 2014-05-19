package io.scalac.seed.vehicle.service

import akka.actor._
import java.util.UUID
import io.scalac.seed.vehicle.domain.VehicleAggregate
import io.scalac.seed.vehicle.domain.VehicleAggregate.ChangeColor
import io.scalac.seed.vehicle.domain.VehicleAggregate.ChangeRegNumber
import io.scalac.seed.vehicle.domain.VehicleAggregate.GetState
import io.scalac.seed.vehicle.domain.VehicleAggregate.Initialize
import io.scalac.seed.vehicle.domain.VehicleAggregate.Remove

object VehicleAggregateManager {
  
  trait Command
  case class RegisterVehicle(regNumber: String, color: String) extends Command
  case class GetVehicle(id: String) extends Command
  case class UpdateRegNumber(id: String, regNumber: String) extends Command
  case class UpdateColor(id: String, color: String) extends Command
  case class DeleteVehicle(id: String) extends Command
  
  def props: Props = Props(new VehicleAggregateManager)

  val maxAggregatesInMemory = 100
  val aggregatesToKillAtOnce = 20
}

class VehicleAggregateManager extends Actor with ActorLogging {

  import VehicleAggregateManager._
  import VehicleAggregate._

  private var actorsPendingTermination: List[ActorRef] = Nil

  def receive = {
    case RegisterVehicle(rn, col) =>
      val id = UUID.randomUUID().toString()
      val agg = create(id)
      agg forward Initialize(rn, col)
    case GetVehicle(id) =>
      val aggregate = findOrCreate(id) 
      aggregate forward GetState
    case UpdateRegNumber(id, regNumber) =>
      val aggregate = findOrCreate(id)
      aggregate forward ChangeRegNumber(regNumber)
    case UpdateColor(id, color) =>
      val aggregate = findOrCreate(id)
      aggregate forward ChangeColor(color)
    case DeleteVehicle(id) =>
      val aggregate = findOrCreate(id)
      aggregate forward Remove
    case Terminated(actor) =>
      actorsPendingTermination = actorsPendingTermination.filterNot(_ == actor)
  }
  
  def create(id: String): ActorRef = {
    log.debug(s"creating actor VehicleAggregate actor ${id}")
    val agg = context.actorOf(VehicleAggregate.props(id), id)
    context watch agg
    agg
  }

  def findOrCreate(id: String): ActorRef =
    context.child(id) match {
      case Some(agg) if !actorsPendingTermination.contains(agg) => agg
      case _ =>
        killChildrenIfNecessary()
        create(id)
    }

  def killChildrenIfNecessary() = {
    val aggregatesCount = context.children.size - actorsPendingTermination.size
    if (aggregatesCount >= maxAggregatesInMemory)
      killAggregates(aggregatesToKillAtOnce)
  }

  def killAggregates(howMany: Int) = {
    val childrenToKill = context.children take howMany
    childrenToKill foreach (_ ! PoisonPill)
    actorsPendingTermination :::= childrenToKill.toList
  }

}