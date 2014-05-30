package io.scalac.seed.vehicle.domain
import akka.actor._
import akka.persistence._
import io.scalac.seed.common.Acknowledge

object VehicleAggregate {

  sealed trait State
  case object UninitializedVehicle extends State
  case object RemovedVehicle extends State

  case class Vehicle(id: String, regNumber: String = "", color: String = "") extends State

  sealed trait Command
  case class Initialize(regNumber: String, color: String) extends Command
  case class ChangeRegNumber(newRegNumber: String) extends Command
  case class ChangeColor(newColor: String) extends Command
  case object Remove extends Command
  case object GetState extends Command
  case object Kill extends Command

  sealed trait Event
  case class VehicleInitialized(regNumber: String, color: String) extends Event
  case class RegNumberChanged(regNumber: String) extends Event
  case class ColorChanged(color: String) extends Event
  case object VehicleRemoved extends Event

  def props(id: String): Props = Props(new VehicleAggregate(id))

  val eventsPerSnapshot = 10
}

class VehicleAggregate(id: String) extends EventsourcedProcessor with ActorLogging {
  
  import VehicleAggregate._

  override def processorId = id
    
  private var state: State = UninitializedVehicle

  private var eventsSinceLastSnapshot = 0
   
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
      state = RemovedVehicle
  }

  private def afterEventPersisted(evt: Event): Unit = {
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      log.debug(s"${eventsPerSnapshot} events reached, saving vehicle snapshot")
      saveSnapshot(state)
      eventsSinceLastSnapshot = 0
    }
    updateAndRespond(evt)
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
    case evt: Event => {
      eventsSinceLastSnapshot += 1
      updateState(evt)
    }
    case SnapshotOffer(_, snapshot: Vehicle) => {
      log.debug("recovering vehicle from snapshot")
      state = snapshot
      state match {
        case UninitializedVehicle => context become initial
        case RemovedVehicle => context become removed
        case _: Vehicle => context become created
      }
    }
  }
  
  val initial: Receive = {
    case Initialize(reg, col) => 
      persist(VehicleInitialized(reg, col))(afterEventPersisted)
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
  }
  
  val created: Receive = {
    case ChangeRegNumber(reg) =>
      persist(RegNumberChanged(reg))(afterEventPersisted)
    case ChangeColor(color) => 
      persist(ColorChanged(color))(afterEventPersisted)
    case Remove =>
      persist(VehicleRemoved)(afterEventPersisted)
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
    case "snap" => saveSnapshot(state)
  }
  
  val removed: Receive = {
    case GetState =>
      respond
    case Kill =>
      self ! PoisonPill
  }

  val receiveCommand: Receive = initial

}