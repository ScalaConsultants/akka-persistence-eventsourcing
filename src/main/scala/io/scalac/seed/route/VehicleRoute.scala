package io.scalac.seed.route

import akka.actor._
import spray.httpx.Json4sSupport
import spray.routing._
import io.scalac.seed.service.{VehicleAggregateManager, AggregateManager}
import io.scalac.seed.domain.VehicleAggregate

object VehicleRoute {
  case class UpdateVehicleData(value: String)
}

trait VehicleRoute extends HttpService with Json4sSupport with PerRequestCreator {

  import VehicleRoute._

  import VehicleAggregateManager._
  
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

  private def serveUpdate(message : AggregateManager.Command): Route =
    ctx => perRequestUpdate[VehicleAggregate.Vehicle](ctx, vehicleAggregateManager, message)

  private def serveRegister(message : AggregateManager.Command): Route =
    ctx => perRequestRegister[VehicleAggregate.Vehicle](ctx, vehicleAggregateManager, message)

  private def serveDelete(message : AggregateManager.Command): Route =
    ctx => perRequestDelete(ctx, vehicleAggregateManager, message)

  private def serveGet(message : AggregateManager.Command): Route =
    ctx => perRequestGet[VehicleAggregate.Vehicle](ctx, vehicleAggregateManager, message)

}