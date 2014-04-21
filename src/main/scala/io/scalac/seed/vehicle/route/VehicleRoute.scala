package io.scalac.seed.vehicle.route

import akka.actor._
import io.scalac.seed._
import io.scalac.seed.common._
import org.json4s.DefaultFormats
import spray.httpx.Json4sSupport
import spray.routing._

case class UpdateVehicleData(value: String)

trait VehicleRoute extends HttpService with Json4sSupport with PerRequestCreator { 
  self: Actor =>

  val json4sFormats = DefaultFormats
    
  val vehicleAggregateManager: ActorRef
  
  val vehicleRoute =
    path("vehicles" / Segment / "regnumber" ) { id =>
      post {
        entity(as[UpdateVehicleData]) { cmd =>
          serve(UpdateRegNumber(id, cmd.value))
        }
      }
    } ~
    path("vehicles" / Segment ) { id =>
      get {
        serve(GetVehicle(id))
      }
    } ~
    path("vehicles") {
      post {
        entity(as[RegisterVehicle]) { cmd =>
          serve(cmd)
        }
      }
    }
    
  def serve(message : RestMessage): Route =
    ctx => perRequest(ctx, vehicleAggregateManager, message)
      
}