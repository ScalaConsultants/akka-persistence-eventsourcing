package io.scalac.seed

import akka.actor._
import spray.http.MediaTypes._
import org.json4s.DefaultFormats
import io.scalac.seed.route.{VehicleRoute}
import io.scalac.seed.service.{VehicleAggregateManager}

class ServiceActor extends Actor with ActorLogging with VehicleRoute {

  val json4sFormats = DefaultFormats

  implicit def actorRefFactory = context

  val vehicleAggregateManager = context.actorOf(VehicleAggregateManager.props)

  def receive =
    runRoute(
      pathPrefix("api") {
        respondWithMediaType(`application/json`) {
          vehicleRoute
        }
      }
    )
    
}