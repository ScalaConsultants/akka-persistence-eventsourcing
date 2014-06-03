package io.scalac.seed.service

import akka.actor._
import io.scalac.seed.domain.AggregateRoot

object AggregateManager {

  trait Command

  val maxChildren = 40
  val childrenToKillAtOnce = 20

  case class PendingCommand(sender: ActorRef, targetProcessorId: String, command: AggregateRoot.Command)
}

trait AggregateManager extends Actor with ActorLogging {

  import AggregateRoot._
  import AggregateManager._

  import scala.collection.immutable._
  private var childrenBeingTerminated: Seq[ActorRef] = Nil
  private var pendingCommands: Seq[PendingCommand] = Nil

  def processCommand: Receive

  def receive = processCommand orElse {
    case Terminated(actor) =>
      childrenBeingTerminated = childrenBeingTerminated filterNot (_ == actor)
      val (commandsForChild, remainingCommands) = pendingCommands partition (_.targetProcessorId == actor.path.name)
      pendingCommands = remainingCommands
      for (PendingCommand(sender, targetProcessorId, command) <- commandsForChild) {
        val child = findOrCreate(targetProcessorId)
        child ! (command, sender)
      }
  }

  def processAggregateCommand(aggregateId: String, command: AggregateRoot.Command) = {
    val maybeChild = context child aggregateId
    maybeChild match {
      case Some(child) if childrenBeingTerminated contains child =>
        pendingCommands :+= PendingCommand(sender(), aggregateId, command)
      case Some(child) =>
        child forward command
      case None =>
        val child = create(aggregateId)
        child forward command
    }
  }

  def findOrCreate(id: String): ActorRef =
    context.child(id) getOrElse create(id)

  def create(id: String): ActorRef = {
    killChildrenIfNecessary()
    val agg = context.actorOf(aggregateProps(id), id)
    context watch agg
    agg
  }

  def aggregateProps(id: String): Props

  def killChildrenIfNecessary() = {
    val childrenCount = context.children.size - childrenBeingTerminated.size
    if (childrenCount >= maxChildren) {
      val childrenNotBeingTerminated = context.children.filterNot(childrenBeingTerminated.toSet)
      val childrenToKill = childrenNotBeingTerminated take childrenToKillAtOnce
      childrenToKill foreach (_ ! Kill)
      childrenBeingTerminated ++= childrenToKill
    }
  }

}