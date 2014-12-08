package io.scalac.seed.domain

import com.github.t3hnar.bcrypt._

object UserAggregate {

  import AggregateRoot._

  case class User(id: String, pass: String = "") extends State

  case class Initialize(pass: String) extends Command
  case class ChangePassword(pass: String) extends Command

  case class UserInitialized(pass: String) extends Event
  case class UserPasswordChanged(pass: String) extends Event
  case object UserRemoved extends Event

}

class UserAggregate(id: String) extends AggregateRoot {


  import UserAggregate._
  import AggregateRoot._

  override def aggregateId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case UserInitialized(pass) =>
      _stateBehavior = created
      _state = User(id, pass)
    case UserPasswordChanged(newPass) =>
      _state match {
        case s: User => _state = s.copy(pass = newPass)
        case _ => //nothing
      }
    case UserRemoved =>
      _stateBehavior = removed
      _state = Removed
  }

  val initial: StateBehavior = {
    case Initialize(pass) =>
      val encryptedPass = pass.bcrypt
      UserInitialized(encryptedPass)
    case GetState =>
      _state
  }

  val created: StateBehavior = {
    case Remove =>
      UserRemoved
    case ChangePassword(newPass) =>
      val newPassEncrypted = newPass.bcrypt
      UserPasswordChanged(newPassEncrypted)
    case GetState =>
      _state
  }

  val removed: StateBehavior = {
    case GetState =>
      _state
  }

  override def restore(stateToRestore: AggregateRoot.State) = {
    this._state = state
    state match {
      case Uninitialized => _stateBehavior = initial
      case Removed => _stateBehavior = removed
      case _: User => _stateBehavior = created
    }
  }

  var _stateBehavior = initial
}