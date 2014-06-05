package io.scalac.seed.service

import akka.actor._
import java.util.UUID
import io.scalac.seed.domain.{UserAggregate, AggregateRoot}

object UserAggregateManager {

  import AggregateManager._

  case class RegisterUser(name: String, pass: String) extends Command
  case class GetUser(name: String) extends Command

  def props: Props = Props(new UserAggregateManager)
}

class UserAggregateManager extends AggregateManager {

  import AggregateRoot._
  import UserAggregateManager._
  import UserAggregate._

  def processCommand = {
    case RegisterUser(name, pass) =>
      val id = "user-" + name
      processAggregateCommand(id, Initialize(pass))
    case GetUser(name) =>
      val id = "user-" + name
      processAggregateCommand(id, GetState)
  }

  override def aggregateProps(id: String) = UserAggregate.props(id)
}