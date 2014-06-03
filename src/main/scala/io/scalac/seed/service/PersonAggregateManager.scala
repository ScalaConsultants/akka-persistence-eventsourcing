package io.scalac.seed.service

import akka.actor._
import java.util.UUID
import io.scalac.seed.domain.{PersonAggregate, AggregateRoot}

object PersonAggregateManager {

  import AggregateManager._

  case class RegisterPerson(name: String) extends Command
  case class GetPerson(id: String) extends Command
  case class DeletePerson(id: String) extends Command
  
  def props: Props = Props(new PersonAggregateManager)
}

class PersonAggregateManager extends AggregateManager {

  import AggregateRoot._
  import PersonAggregateManager._
  import PersonAggregate._

  def processCommand = {
    case RegisterPerson(name) =>
      val id = UUID.randomUUID().toString()
      processAggregateCommand(id, Initialize(name))
    case GetPerson(id) =>
      processAggregateCommand(id, GetState)
    case DeletePerson(id) =>
      processAggregateCommand(id, Remove)
  }

  override def aggregateProps(id: String) = PersonAggregate.props(id)
}