package io.scalac.seed

import akka.actor._
import spray.http.MediaTypes._
import io.scalac.seed.vehicle.route.VehicleRoute
import io.scalac.seed.vehicle.service.VehicleAggregateManager

class ServiceActor extends Actor with ActorLogging with VehicleRoute {
  
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