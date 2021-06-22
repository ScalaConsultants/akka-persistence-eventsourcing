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

trait UserAggregateRoot extends AggregateRoot {
  def calculateState(id: String, state: State, event: Event): State = (state, event) match {
    case (Uninitialized, UserInitialized(pass)) ⇒ User(id, pass)
    case (s: User, UserPasswordChanged(newPass)) ⇒ s.copy(pass = newPass)
    case (_, UserPasswordChanged(_)) ⇒ state
    case (_, UserRemoved) ⇒ Removed
    case _ ⇒ ???
  }

  def executeQuery(id: String, state: State, query: Command): State = state

  def calculateEvents(id: String, state: State, command: Command): Event = (state, command) match {
    case (Uninitialized, Initialize(pass)) ⇒
      val encryptedPass = pass.bcrypt
      UserInitialized(encryptedPass)
    case (_ : User, ChangePassword(newPass)) ⇒
      val newPassEncrypted = newPass.bcrypt
      UserPasswordChanged(newPassEncrypted)
    case (_ : User, Remove) ⇒ UserRemoved
    case _ ⇒ ???
  }
}
