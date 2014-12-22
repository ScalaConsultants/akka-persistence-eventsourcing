package io.scalac.seed.service

import io.scalac.seed.domain.AggregateRoot.GetState
import io.scalac.seed.domain._
import io.scalac.seed.service.CommandAdapter.Command

object UserCommandAdapter {

  case class RegisterUser(name: String, pass: String) extends Command
  case class GetUser(name: String) extends Command
  case class ChangeUserPassword(id: String, pass: String) extends Command

}

class UserCommandAdapter extends CommandAdapter {

  import UserCommandAdapter._
  import UserAggregate._

  def adapt(command: Command) : (String, AggregateRoot.Command) = command match {
    case RegisterUser(name, pass) => (nameToId(name), Initialize(pass))
    case GetUser(name) => (nameToId(name), GetState)
    case ChangeUserPassword(id, pass) => (id, ChangePassword(pass))
  }

  def nameToId(name:String):String = "user-" + name

}
