package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import io.scalac.seed.common.Acknowledge

object AggregateRoot {

  trait State
  case object Uninitialized extends State
  case object Removed extends State

  trait Event

  trait Command
  case object Remove extends Command
  case object Kill extends Command
  case object GetState extends Command

  val eventsPerSnapshot = 10
}

trait AggregateRoot extends EventsourcedProcessor with ActorLogging {

  import AggregateRoot._

  override def processorId: String

  var state: State = Uninitialized

  private var eventsSinceLastSnapshot = 0
   
  def updateState(evt: Event): Unit

  def afterEventPersisted(evt: Event): Unit = {
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      log.debug(s"${eventsPerSnapshot} events reached, saving snapshot")
      saveSnapshot(state)
      eventsSinceLastSnapshot = 0
    }
    updateAndRespond(evt)
  }

  def updateAndRespond(evt: Event): Unit = {
    updateState(evt)
    respond
  }

  def respond: Unit = {
    sender() ! state
    context.parent ! Acknowledge(processorId)
  }

  val receiveRecover: Receive = {
    case evt: Event =>
      eventsSinceLastSnapshot += 1
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State)

}