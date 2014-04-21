package io.scalac.seed.vehicle.domain
import akka.actor._
import akka.persistence._
import io.scalac.seed.common.RestMessage

case class VehicleState(id: String, regNumber: String = "", color: String = "", ownerId: String = "")

object VehicleAggregate {
  
  sealed trait Command
  case class SetState(regNumber: String, color: String, ownerId: String) extends Command
  case class ChangeRegNumber(newRegNumber: String) extends Command
  case class ChangeColor(newColor: String) extends Command
  case class ChangeOwner(newOwnerId: String) extends Command
  case class GetState() extends Command

  sealed trait Event
  case class RegNumberChanged(regNumber: String) extends Event
  case class ColorChanged(color: String) extends Event
  case class OwnerChanged(ownerId: String) extends Event

  def props(id: String): Props = Props(new VehicleAggregate(id))
}

class VehicleAggregate(id: String) extends EventsourcedProcessor {
  
  import VehicleAggregate._
  
  override def processorId = id
    
  var state: VehicleState = VehicleState(id)
   
  def updateState(evt: Event): Unit = evt match {
    case RegNumberChanged(reg) => state = state.copy(regNumber = reg)
    case ColorChanged(col) => state = state.copy(color = col)
    case OwnerChanged(oid) => state = state.copy(ownerId = oid)
  }
    
  val receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: VehicleState) => state = snapshot
  }
    
  val receiveCommand: Receive = {
    case SetState(reg, col, owner) => 
      val sender = context.sender
      persist(RegNumberChanged(reg))(updateState)
      persist(ColorChanged(col))(updateState)
      persist(OwnerChanged(owner))(updateState)
    case ChangeRegNumber(reg) => persist(RegNumberChanged(reg))(updateState)
    case ChangeColor(color) => persist(ColorChanged(color))(updateState)
    case ChangeOwner(owner) => persist(OwnerChanged(owner))(updateState)
    case GetState() =>
      val sender = context.sender
      sender ! state.copy()
    case "snap" => saveSnapshot(state)
    case "print" => println(state)
  }
}