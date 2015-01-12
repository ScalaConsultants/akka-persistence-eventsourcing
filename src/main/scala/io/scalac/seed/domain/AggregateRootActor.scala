package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import io.scalac.seed.common.Acknowledge
import io.scalac.seed.domain.AggregateRoot.{Command, Event, State}

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

trait AggregateRootAdapter {

  def acceptCommand(aggregateId: String, state: State, eventReaction: (State, Event) ⇒ Unit, queryReaction: State ⇒ Unit): PartialFunction[Command, Unit]

  def recover(aggregateId: String, state: State, event: Event): State
}


/**
 * Base class for other aggregates.
 * It includes such functionality as: snapshot management, publishing applied events to Event Bus, handling processor recovery.
 *
 */
class AggregateRootActor(val id: String, var state: State, var aggregate: AggregateRootAdapter) extends PersistentActor with ActorLogging {


  import AggregateRootActor._
  import AggregateRoot._

  override def persistenceId = id

  private var eventsSinceLastSnapshot = 0


  /**
   * This method should be used as a callback handler for persist() method.
   *
   * @param evt Event that has been persisted
   */
  protected def afterEventPersisted(newState: State)(evt: Event): Unit = {
    updateAndRespond(newState)
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      log.debug("{} events reached, saving snapshot", eventsPerSnapshot)
      saveSnapshot(aggregate)
      eventsSinceLastSnapshot = 0
    }
    publish(evt)
  }

  private def updateAndRespond(newState: State): Unit = {
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

  private def publish(event: Event) =
    context.system.eventStream.publish(event)

  override val receiveRecover: Receive = {
    case evt: Event =>
      eventsSinceLastSnapshot += 1
      updateAggregate(aggregate.recover(id, state, evt))
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    updateAggregate(state)
  }

  def updateAggregate(state: State) {
    this.state = state
  }

  def afterEventCallback(newState: State, event: Event) {
    persist(event)(afterEventPersisted(newState))
  }

  val receiveCommand: Receive = {
    case command: Command ⇒ {
      aggregate.acceptCommand(id, state, afterEventCallback, respond)(command)
    }
    case KillAggregate ⇒ context.stop(self)
  }

}