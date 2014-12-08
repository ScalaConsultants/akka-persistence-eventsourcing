package io.scalac.seed.domain

import akka.actor.Props

trait AggregatePropsProvider {
  def aggregateRootProps(id: String): Props // This name is so fancy because of possible namespace pollution
                                            // caused by mixins, so maybe composition should be used here?
}

trait UserAggregatePropsProvider extends AggregatePropsProvider{
  def aggregateRootProps(id: String): Props = Props(new UserAggregateActor(id))
}

object UserAggregateActor extends UserAggregatePropsProvider

class UserAggregateActor(id: String) extends UserAggregate(id) with AggregateRootActor

trait VehicleAggregatePropsProvider extends AggregatePropsProvider{
  def aggregateRootProps(id: String): Props = Props(new VehicleAggregateActor(id))
}

object VehicleAggregateActor extends VehicleAggregatePropsProvider

class VehicleAggregateActor(id: String) extends VehicleAggregate(id) with AggregateRootActor