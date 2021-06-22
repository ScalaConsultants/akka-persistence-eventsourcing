package io.scalac.seed.domain

import akka.actor.Props

trait AggregatePropsProvider {
  def aggregateRootProps(id: String): Props // This name is so fancy because of possible namespace pollution
  // caused by mixins, so maybe composition should be used here?
}

object UserAggregateRootAdapter extends DefaultAggregateRootAdapter with UserAggregateRoot

object VehicleAggregateRootAdapter extends DefaultAggregateRootAdapter with VehicleAggregateRoot

class UserAggregateActor(id: String, state: AggregateRoot.State, adapter: DefaultAggregateRootAdapter)
  extends AggregateRootActor(id, state, adapter)

class VehicleAggregateActor(id: String, state: AggregateRoot.State, adapter: DefaultAggregateRootAdapter)
  extends AggregateRootActor(id, state, adapter)

trait UserAggregatePropsProvider extends AggregatePropsProvider {
  def aggregateRootProps(id: String): Props = Props(
    new UserAggregateActor(id, AggregateRoot.Uninitialized, UserAggregateRootAdapter)
  )
}

trait VehicleAggregatePropsProvider extends AggregatePropsProvider {
  def aggregateRootProps(id: String): Props = Props(
    new VehicleAggregateActor(id, AggregateRoot.Uninitialized, VehicleAggregateRootAdapter)
  )
}

object UserAggregateActor extends UserAggregatePropsProvider

object VehicleAggregateActor extends VehicleAggregatePropsProvider
