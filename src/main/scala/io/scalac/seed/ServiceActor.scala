package io.scalac.seed

import akka.actor._
import spray.http.MediaTypes._
import spray.routing._
import spray.routing.directives.RespondWithDirectives._
import io.scalac.seed.vehicle.route.VehicleRoute
import io.scalac.seed.vehicle.route.VehicleAggregateManager

class ServiceActor extends HttpService with Actor with ActorLogging 
  with VehicleRoute {
  
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