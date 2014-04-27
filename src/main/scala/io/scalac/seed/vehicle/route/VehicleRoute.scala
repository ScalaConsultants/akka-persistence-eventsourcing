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

  import VehicleAggregateManager._
  
  val json4sFormats = DefaultFormats
    
  val vehicleAggregateManager: ActorRef
  
  val vehicleRoute =
    path("vehicles" / Segment / "regnumber" ) { id =>
      post {
        entity(as[UpdateVehicleData]) { cmd =>
          serveUpdate(UpdateRegNumber(id, cmd.value))
        }
      }
    } ~
    path("vehicles" / Segment / "color" ) { id =>
      post {
        entity(as[UpdateVehicleData]) { cmd =>
          serveUpdate(UpdateColor(id, cmd.value))
        }
      }
    } ~
    path("vehicles" / Segment ) { id =>
      get {
        serveGet(GetVehicle(id))
      } ~
      delete {
        serveDelete(DeleteVehicle(id))
      }
    } ~
    path("vehicles") {
      post {
        entity(as[RegisterVehicle]) { cmd =>
          serveRegister(cmd)
        }
      }
    }
    
  def serveUpdate(message : VehicleAggregateManager.Command): Route =
    ctx => perRequestUpdate(ctx, vehicleAggregateManager, message)

  def serveRegister(message : VehicleAggregateManager.Command): Route =
    ctx => perRequestRegister(ctx, vehicleAggregateManager, message)

  def serveDelete(message : VehicleAggregateManager.Command): Route =
    ctx => perRequestDelete(ctx, vehicleAggregateManager, message)

  def serveGet(message : VehicleAggregateManager.Command): Route =
    ctx => perRequestGet(ctx, vehicleAggregateManager, message)

}