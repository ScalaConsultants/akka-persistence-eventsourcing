package io.scalac.seed.vehicle.domain
import akka.actor._
import akka.persistence._
import io.scalac.seed.common.Acknowledge

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

class VehicleAggregate(id: String) extends EventsourcedProcessor with ActorLogging {
  
  import VehicleAggregate._
  import Vehicle._
  
  override def processorId = id
    
  var state: State = EmptyVehicle
   
  private def updateState(evt: Event): Unit = evt match {
    case VehicleInitialized(reg, col) =>
      context.become(created)
      state = Vehicle(id, reg, col)
    case RegNumberChanged(reg) => state match {
      case s: Vehicle => state = s.copy(regNumber = reg) 
      case _ => //nothing
    }
    case ColorChanged(col) => state match { 
      case s: Vehicle => state = s.copy(color = col)
      case _ => //nothing
    }
    case VehicleRemoved =>
      context.become(removed)
      state = EmptyVehicle
  }
  
  private def updateAndRespond(evt: Event): Unit = {
    updateState(evt)
    respond
  }
  
  private def respond: Unit = {
    log.debug("*** " + sender())
    log.debug("**** " + context.parent)
    sender() ! state
    context.parent ! Acknowledge(id)
  }
  
  val receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: Vehicle) => state = snapshot
  }
  
  val initial: Receive = {
    case Initialize(reg, col) => 
      persist(VehicleInitialized(reg, col))(updateAndRespond)
    case GetState =>
      respond
  }
  
  val created: Receive = {
    case ChangeRegNumber(reg) => 
      persist(RegNumberChanged(reg))(updateAndRespond)
    case ChangeColor(color) => 
      persist(ColorChanged(color))(updateAndRespond)
    case Remove =>
      persist(VehicleRemoved)(updateAndRespond)
    case GetState =>
      respond
    case "snap" => saveSnapshot(state)
  }
  
  val removed: Receive = {
    case GetState =>
      respond
  }

  val receiveCommand: Receive = initial

}