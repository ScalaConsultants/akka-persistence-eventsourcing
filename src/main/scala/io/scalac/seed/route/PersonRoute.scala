package io.scalac.seed.route

import akka.actor._
import spray.httpx.Json4sSupport
import spray.routing._
import io.scalac.seed.service.{PersonAggregateManager, AggregateManager}
import io.scalac.seed.domain.PersonAggregate

trait PersonRoute extends HttpService with Json4sSupport with PerRequestCreator {

  import PersonAggregateManager._

  val personAggregateManager: ActorRef
  
  val personRoute =
    path("person" / Segment ) { id =>
      get {
        serveGet(GetPerson(id))
      } ~
      delete {
        serveDelete(DeletePerson(id))
      }
    } ~
    path("person") {
      post {
        entity(as[RegisterPerson]) { cmd =>
          serveRegister(cmd)
        }
      }
    }
    
  private def serveRegister(message : AggregateManager.Command): Route =
    ctx => perRequestRegister[PersonAggregate.Person](ctx, personAggregateManager, message)

  private def serveDelete(message : AggregateManager.Command): Route =
    ctx => perRequestDelete(ctx, personAggregateManager, message)

  private def serveGet(message : AggregateManager.Command): Route =
    ctx => perRequestGet[PersonAggregate.Person](ctx, personAggregateManager, message)

}