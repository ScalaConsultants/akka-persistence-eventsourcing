akka-persistence-eventsourcing
==============================

Example project with a simple REST API to a domain model persisted using akka-persistence with event sourcing.

To start the spray-can server from sbt:
> re-start

To stop:
> re-stop


### Project summary

- simple CRUD-like REST API
- "actor per request" model as shown in spray-actor-per-request Activator template
- simple domain model representing a Vehicle
- akka-persistence event sourcing used to track changes to the domain model

### //TODO

- implement the akka-persistence snapshots
- VehicleAggregateManager should kill the VehicleAggregate after the command is finished or after some arbitrary period of time, as described on akka-user list:

> ...the high level description:
> - all messages are sent via the Manager actor, which creates child Aggregate instances on demand
> - when receiving a message the Manager extract the Aggregate identifier from the message
> - the Manager creates a new child Aggregate actor if it doesn't exist, and then forwards the message to the Aggregate
> - the Aggregate can passivate itself by sending a Passivate message to the parent Manager, which then sends PoisonPill to the Aggregate
> - in-between receiving Passivate and Terminated the Manager will buffer all incoming messages for the passivating Aggregate
> - when receiving Terminated it will flush the buffer for the Aggregate, which can result in activation again

- use the ReactiveMongo plugin instead of the default LevelDB ???
- how to show the list of all vehicles ???