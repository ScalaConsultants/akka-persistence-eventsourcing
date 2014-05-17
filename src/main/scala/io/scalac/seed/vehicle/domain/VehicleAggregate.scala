package io.scalac.seed.vehicle.domain
import akka.actor._
import akka.persistence._

object Vehicle {
  trait State
  case object EmptyState extends State
  case object RemovedState extends State
  case class VehicleState(id: String, regNumber: String = "", color: String = "") extends State
}

object VehicleAggregate {
  
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
    
  var state: VehicleState = VehicleState(id)
   
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
    
  val receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: VehicleState) => state = snapshot
  }
  
  val initial: Receive = {
    case Initialize(reg, col) => 
      persist(VehicleInitialized(reg, col))(updateState)
    case GetState =>
      sender() ! EmptyState
  }
  
  val created: Receive = {
    case ChangeRegNumber(reg) => persist(RegNumberChanged(reg))(updateState)
    case ChangeColor(color) => persist(ColorChanged(color))(updateState)    
    case Remove =>
      persist(VehicleRemoved)(updateState)
    case GetState =>
      sender() ! state
    case "snap" => saveSnapshot(state)
  }
  
  val removed: Receive = {
    case GetState =>
      sender() ! EmptyState
  }

  val receiveCommand: Receive = initial

}