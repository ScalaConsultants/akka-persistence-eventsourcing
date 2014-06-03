package io.scalac.seed

import akka.actor._
import spray.http.MediaTypes._
import org.json4s.DefaultFormats
import io.scalac.seed.route.{VehicleRoute, PersonRoute}
import io.scalac.seed.service.{VehicleAggregateManager, PersonAggregateManager}

class ServiceActor extends Actor with ActorLogging with VehicleRoute with PersonRoute {

  val json4sFormats = DefaultFormats

  implicit def actorRefFactory = context

  val vehicleAggregateManager = context.actorOf(VehicleAggregateManager.props)

  val personAggregateManager = context.actorOf(PersonAggregateManager.props)

  def receive =
    runRoute(
      pathPrefix("api") {
        respondWithMediaType(`application/json`) {
          vehicleRoute ~ personRoute
        }
      }
    )
    
}