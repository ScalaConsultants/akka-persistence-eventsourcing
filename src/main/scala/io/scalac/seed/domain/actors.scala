package io.scalac.seed.domain

import akka.actor.Props

trait AggregatePropsProvider {
  def aggregateRootProps(id: String): Props // This name is so fancy because of possible namespace pollution
  // caused by mixins, so maybe composition should be used here?
}

trait AggregateRootProvider {
  def aggregateRoot(id: String, state: AggregateRoot.State): AggregateRoot
}

trait UserAggregatePropsProvider extends AggregatePropsProvider {
  def aggregateRootProps(id: String): Props = Props(
    new UserAggregateActor(
      UserAggregateProvider,
      UserAggregateProvider.aggregateRoot(id, AggregateRoot.Uninitialized)
    )
  )
}

object UserAggregateActor extends UserAggregatePropsProvider

class UserAggregateActor(aggregateProvider: AggregateRootProvider, aggregate: AggregateRoot)
  extends AggregateRootActor(aggregateProvider, aggregate)

trait VehicleAggregatePropsProvider extends AggregatePropsProvider {
  def aggregateRootProps(id: String): Props = Props(
    new VehicleAggregateActor(
      VehicleAggregateProvider,
      VehicleAggregateProvider.aggregateRoot(id, AggregateRoot.Uninitialized)
    )
  )
}

object VehicleAggregateActor extends VehicleAggregatePropsProvider

class VehicleAggregateActor(aggregateProvider: AggregateRootProvider, aggregate: AggregateRoot)
  extends AggregateRootActor(aggregateProvider, aggregate)