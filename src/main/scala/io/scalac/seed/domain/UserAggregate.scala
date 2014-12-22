package io.scalac.seed.domain

import com.github.t3hnar.bcrypt._
import io.scalac.seed.domain.AggregateRoot._
import io.scalac.seed.domain.UserAggregate._

object UserAggregate {

  import AggregateRoot._

  case class User(id: String, pass: String = "") extends State

  case class Initialize(pass: String) extends Command
  case class ChangePassword(pass: String) extends Command

  case class UserInitialized(pass: String) extends Event
  case class UserPasswordChanged(pass: String) extends Event
  case object UserRemoved extends Event

}

object UserAggregateProvider extends AggregateRootProvider{
  def aggregateRoot(id: String, state: State): AggregateRoot = state match {
    case Uninitialized ⇒ new UserAggregateInitial(id, state)
    case Removed ⇒ new UserAggregateRemoved(id, state)
    case _: User ⇒ new UserAggregateCreated(id, state)
  }

}

abstract class UserAggregate(aggregateId: String, state: State) extends AggregateRoot(aggregateId, state) {
  override def updateState(evt: Event): State = evt match {
    case UserInitialized(pass) =>
      User(aggregateId, pass)
    case UserPasswordChanged(newPass) =>
      state match {
        case s: User => s.copy(pass = newPass)
        case _ => state
      }
    case UserRemoved =>
      Removed
  }
}

class UserAggregateInitial(aggregateId: String, state: State) extends UserAggregate(aggregateId, state) {
  override val stateBehavior: StateBehavior = {
    case Initialize(pass) =>
      val encryptedPass = pass.bcrypt
      UserInitialized(encryptedPass)
    case GetState =>
      state
  }
}

class UserAggregateCreated(aggregateId: String, state: State) extends UserAggregate(aggregateId, state) {
  override val stateBehavior: StateBehavior = {
    case Remove =>
      UserRemoved
    case ChangePassword(newPass) =>
      val newPassEncrypted = newPass.bcrypt
      UserPasswordChanged(newPassEncrypted)
    case GetState =>
      state
  }
}

class UserAggregateRemoved(aggregateId: String, state: State) extends UserAggregate(aggregateId, state) {
  override val stateBehavior: StateBehavior = {
    case GetState =>
      state
  }
}