package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import _root_.io.scalac.seed.common.Acknowledge

import scala.reflect._

object AggregateRootActor {


  /**
   * We don't want the aggregate to be killed if it hasn't fully restored yet,
   * thus we need some non AutoReceivedMessage that can be handled by akka persistence.
   */
  case object KillAggregate

  /**
   * Specifies how many events should be processed before new snapshot is taken.
   */
  val eventsPerSnapshot = 10
}

trait AggregateRootAdapter[S, C, E <: AnyRef] {

  def acceptCommand(aggregateId: String, state: S, eventReaction: (S, E) ⇒ Unit, queryReaction: S ⇒ Unit): PartialFunction[C, Unit]

  def recover(aggregateId: String, state: S, event: E): S
}


/**
 * Base class for other aggregates.
 * It includes such functionality as: snapshot management, publishing applied events to Event Bus, handling processor recovery.
 *
 */
class AggregateRootActor[S: ClassTag, C: ClassTag, E <: AnyRef : ClassTag](val id: String,
                                                                           var state: S,
                                                                           var aggregate: AggregateRootAdapter[S, C, E])
  extends PersistentActor with ActorLogging {


  import AggregateRootActor._

  override def persistenceId = id

  private var eventsSinceLastSnapshot = 0


  /**
   * This method should be used as a callback handler for persist() method.
   *
   * @param evt Event that has been persisted
   */
  protected def afterEventPersisted(newState: S)(evt: E): Unit = {
    updateAndRespond(newState)
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      log.debug("{} events reached, saving snapshot", eventsPerSnapshot)
      saveSnapshot(aggregate)
      eventsSinceLastSnapshot = 0
    }
    publish(evt)
  }

  private def updateAndRespond(newState: S): Unit = {
    updateAggregate(newState)
    respond()
  }

  protected def respond(): Unit = {
    respond(state)
  }

  def respond(response: Any) {
    sender() ! response // This can be inefficient if state is huge and sent outside jvm.
    context.parent ! Acknowledge(persistenceId)
  }

  private def publish(event: E) =
    context.system.eventStream.publish(event)

  override val receiveRecover: Receive = {
    case evt: E =>
      eventsSinceLastSnapshot += 1
      updateAggregate(aggregate.recover(id, state, evt))
    case SnapshotOffer(metadata, state: S) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: S) = {
    updateAggregate(state)
  }

  def updateAggregate(state: S) {
    this.state = state
  }

  def afterEventCallback(newState: S, event: E) {
    persist(event)(afterEventPersisted(newState))
  }

  val receiveCommand: Receive = {
    case command: C ⇒ {
      aggregate.acceptCommand(id, state, afterEventCallback, respond)(command)
    }
    case KillAggregate ⇒ context.stop(self)
  }

}