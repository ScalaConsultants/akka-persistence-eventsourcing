package io.scalac.seed.domain

import akka.actor._
import akka.persistence.SnapshotMetadata
import com.github.t3hnar.bcrypt._
import io.scalac.seed.domain.AggregateRoot._

object UserAggregate {

  import AggregateRoot._

  case class User(id: String, pass: String = "") extends State

  case class Initialize(pass: String) extends Command
  case class ChangePassword(pass: String) extends Command

  case class UserInitialized(pass: String) extends Event
  case class UserPasswordChanged(pass: String) extends Event
  case object UserRemoved extends Event

  def props(id: String): Props = Props(new UserAggregate(id))
}

class UserAggregate(id: String) extends AggregateRoot {

  import UserAggregate._

  override def persistenceId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case UserInitialized(pass) =>
      context.become(created)
      state = User(id, pass)
    case UserPasswordChanged(newPass) =>
      state match {
        case s: User => state = s.copy(pass = newPass)
        case _ => //nothing
      }
    case UserRemoved =>
      context.become(removed)
      state = Removed
  }

  val initial: Receive = {
    case Initialize(pass) =>
      val encryptedPass = pass.bcrypt
      persist(UserInitialized(encryptedPass))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val created: Receive = {
    case Remove =>
      persist(UserRemoved)(afterEventPersisted)
    case ChangePassword(newPass) =>
      val newPassEncrypted = newPass.bcrypt
      persist(UserPasswordChanged(newPassEncrypted))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val removed: Receive = {
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val receiveCommand: Receive = initial

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: AggregateRoot.State) = {
    this.state = state
    state match {
      case Uninitialized => context become initial
      case Removed => context become removed
      case _: User => context become created
    }
  }

}