package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import io.scalac.seed.common.Acknowledge

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

/**
 * Base class for other aggregates.
 * It includes such functionality as: snapshot management, publishing applied events to Event Bus, handling processor recovery.
 *
 */
class AggregateRootActor(val aggregateProvider: AggregateRootProvider, var aggregate: AggregateRoot) extends PersistentActor with ActorLogging {


  import AggregateRootActor._
  import AggregateRoot._

  override def persistenceId = aggregate.aggregateId

  private var eventsSinceLastSnapshot = 0


  /**
   * This method should be used as a callback handler for persist() method.
   *
   * @param evt Event that has been persisted
   */
  protected def afterEventPersisted(newState: State)(evt: Event): Unit = {
    eventsSinceLastSnapshot += 1
    updateAndRespond(newState)
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
    respond(aggregate.state)
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
      updateAggregate(aggregate.updateState(evt))
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    updateAggregate(state)
  }

  def updateAggregate(state: State) {
    aggregate = aggregateProvider.aggregateRoot(aggregate.aggregateId, state)
  }

  val receiveCommand: Receive = {
    case command: Command ⇒ {
      aggregate.acceptEvent.apply(command) match {
        case AcceptedEvent(event, newState) ⇒ persist(event)(afterEventPersisted (newState))
        case AcceptedQuery(result) ⇒ respond(result)
      }
    }
    case KillAggregate ⇒ context.stop(self)
  }

}