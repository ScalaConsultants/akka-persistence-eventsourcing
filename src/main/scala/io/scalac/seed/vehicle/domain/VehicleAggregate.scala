package io.scalac.seed.vehicle.domain
import akka.actor._
import akka.persistence._

object VehicleAggregate {

  sealed trait State
  case object EmptyVehicle extends State
  case class Vehicle(id: String, regNumber: String = "", color: String = "") extends State

  sealed trait Command
  case class Initialize(regNumber: String, color: String) extends Command
  case class ChangeRegNumber(newRegNumber: String) extends Command
  case class ChangeColor(newColor: String) extends Command
  case object Remove extends Command
  case object GetState extends Command

  sealed trait Event
  case class VehicleInitialized(regNumber: String, color: String) extends Event
  case class RegNumberChanged(regNumber: String) extends Event
  case class ColorChanged(color: String) extends Event
  case object VehicleRemoved extends Event

  def props(id: String): Props = Props(new VehicleAggregate(id))
}

class VehicleAggregate(id: String) extends EventsourcedProcessor {
  
  import VehicleAggregate._
  import Vehicle._
  
  override def processorId = id
    
  var state: Vehicle = Vehicle(id)
   
  private def updateState(evt: Event): Unit = evt match {
    case VehicleInitialized(reg, col) =>
      context.become(created)
      state = state.copy(regNumber = reg, color = col)
    case RegNumberChanged(reg) => 
      state = state.copy(regNumber = reg)
    case ColorChanged(col) => 
      state = state.copy(color = col)
    case VehicleRemoved =>
      context.become(removed)
  }
  
  private def updateAndRespond(evt: Event): Unit = {
    updateState(evt)
    sender() ! state
  }
  
  val receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: Vehicle) => state = snapshot
  }
  
  val initial: Receive = {
    case Initialize(reg, col) => 
      persist(VehicleInitialized(reg, col))(updateAndRespond)
    case GetState =>
      sender() ! EmptyVehicle
  }
  
  val created: Receive = {
    case ChangeRegNumber(reg) => 
      persist(RegNumberChanged(reg))(updateAndRespond)
    case ChangeColor(color) => 
      persist(ColorChanged(color))(updateAndRespond)
    case Remove =>
      persist(VehicleRemoved)(updateAndRespond)
    case GetState =>
      sender() ! state
    case "snap" => saveSnapshot(state)
  }
  
  val removed: Receive = {
    case GetState =>
      sender() ! EmptyVehicle
  }

  val receiveCommand: Receive = initial

}