package io.scalac.seed

import akka.actor._
import spray.http.MediaTypes._
import org.json4s.DefaultFormats
import io.scalac.seed.route.{VehicleRoute, UserRoute}
import io.scalac.seed.service.{VehicleAggregateManager, UserAggregateManager}

class ServiceActor extends Actor with ActorLogging with VehicleRoute with UserRoute {

  val json4sFormats = DefaultFormats

  implicit def actorRefFactory = context

  val vehicleAggregateManager = context.actorOf(VehicleAggregateManager.props)

  val userAggregateManager = context.actorOf(UserAggregateManager.props)

  def receive =
    runRoute(
      pathPrefix("api") {
        respondWithMediaType(`application/json`) {
          vehicleRoute ~ userRoute
        }
      }
    )
    
}