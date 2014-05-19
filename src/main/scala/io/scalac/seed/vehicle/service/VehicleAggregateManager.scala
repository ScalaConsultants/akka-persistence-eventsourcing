package io.scalac.seed.vehicle.service

import akka.actor._
import java.util.UUID
import io.scalac.seed.vehicle.domain.VehicleAggregate

object VehicleAggregateManager {
  
  trait Command
  case class RegisterVehicle(regNumber: String, color: String) extends Command
  case class GetVehicle(id: String) extends Command
  case class UpdateRegNumber(id: String, regNumber: String) extends Command
  case class UpdateColor(id: String, color: String) extends Command
  case class DeleteVehicle(id: String) extends Command
  
  def props: Props = Props(new VehicleAggregateManager)

  val maxChildren = 40
  val childrenToKillAtOnce = 20
}

class VehicleAggregateManager extends Actor with ActorLogging {

  import VehicleAggregateManager._
  import VehicleAggregate._

  case class PendingCommand(sender: ActorRef, targetProcessorId: String, command: VehicleAggregate.Command)

  private var childrenBeingTerminated: Seq[ActorRef] = Nil
  private var pendingCommands: Seq[PendingCommand] = Nil

  def receive = {
    case RegisterVehicle(rn, col) =>
      val id = UUID.randomUUID().toString()
      processCommand(id, Initialize(rn, col))
    case GetVehicle(id) =>
      processCommand(id, GetState)
    case UpdateRegNumber(id, regNumber) =>
      processCommand(id, ChangeRegNumber(regNumber))
    case UpdateColor(id, color) =>
      processCommand(id, ChangeColor(color))
    case DeleteVehicle(id) =>
      processCommand(id, Remove)
    case Terminated(actor) =>
      log.debug(s"child termination finished, optionally recreating and sending cached commands (if any)")
      childrenBeingTerminated = childrenBeingTerminated filterNot (_ == actor)
      val (commandsForChild, remainingCommands) = pendingCommands partition (_.targetProcessorId == actor.path.name)
      pendingCommands = remainingCommands
      for (PendingCommand(sender, targetProcessorId, command) <- commandsForChild) {
        val child = findOrCreate(targetProcessorId)
        child ! (command, sender)
      }
  }

  def processCommand(id: String, command: VehicleAggregate.Command) = {
    val maybeChild = context.child(id)
    maybeChild match {
      case Some(child) if childrenBeingTerminated contains child =>
        log.debug(s"processing command for child currently being terminated, putting it to cache")
        pendingCommands :+= PendingCommand(sender(), id, command)
      case Some(child) =>
        log.debug(s"processing command for existing child, forwarding")
        child forward command
      case None =>
        log.debug(s"processing command for non-existing child, creating")
        val child = create(id)
        child forward command
    }
  }

  def findOrCreate(id: String): ActorRef =
    context.child(id) getOrElse create(id)

  def create(id: String): ActorRef = {
    log.debug(s"creating actor VehicleAggregate actor ${id}, children count is ${context.children.size}")
    killChildrenIfNecessary()
    val agg = context.actorOf(VehicleAggregate.props(id), id)
    context watch agg
    agg
  }

  def killChildrenIfNecessary() = {
    val childrenCount = context.children.size - childrenBeingTerminated.size
    if (childrenCount >= maxChildren) {
      log.debug(s"max children count exceeded (children count is ${childrenCount}), killing some of them")
      val childrenNotBeingTerminated = context.children.filterNot(childrenBeingTerminated.toSet)
      val childrenToKill = childrenNotBeingTerminated take childrenToKillAtOnce
      childrenToKill foreach (_ ! VehicleAggregate.Kill)
      childrenBeingTerminated ++= childrenToKill
    }
  }

}