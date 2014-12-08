package io.scalac.seed.service

import akka.actor.Props
import io.scalac.seed.domain.{VehicleAggregatePropsProvider, UserAggregatePropsProvider}

object UserAggregateManager {
  def props: Props = Props(new UserAggregateManager)
}

class UserAggregateManager extends UserCommandAdapter with UserAggregatePropsProvider with AggregateManager

object VehicleAggregateManager {
  def props: Props = Props(new VehicleAggregateManager)
}

class VehicleAggregateManager extends VehicleCommandAdapter with VehicleAggregatePropsProvider with AggregateManager