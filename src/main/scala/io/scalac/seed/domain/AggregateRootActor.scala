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
trait AggregateRootActor extends PersistentActor with ActorLogging {
  this: AggregateRoot ⇒


  import AggregateRootActor._
  import AggregateRoot._

  override def persistenceId = aggregateId

  private var eventsSinceLastSnapshot = 0


  /**
   * This method should be used as a callback handler for persist() method.
   *
   * @param evt Event that has been persisted
   */
  protected def afterEventPersisted(evt: Event): Unit = {
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      log.debug("{} events reached, saving snapshot", eventsPerSnapshot)
      saveSnapshot(state)
      eventsSinceLastSnapshot = 0
    }
    updateAndRespond(evt)
    publish(evt)
  }

  private def updateAndRespond(evt: Event): Unit = {
    updateState(evt)
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
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    restore(state)
  }

  val receiveCommand: Receive = {
    case command: Command ⇒ {
      stateBehavior.apply(command) match {
        case event: Event ⇒ persist(event)(afterEventPersisted)
        case result ⇒ respond(result)
      }
    }
    case KillAggregate ⇒ context.stop(self)
  }

}