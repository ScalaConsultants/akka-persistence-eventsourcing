package io.scalac.seed

import spray.http.StatusCodes
import org.scalatest.{Matchers, FlatSpec}
import spray.testkit.ScalatestRouteTest
import io.scalac.seed.vehicle.service.VehicleAggregateManager
import io.scalac.seed.vehicle.service.VehicleAggregateManager.{GetVehicle, RegisterVehicle}
import io.scalac.seed.vehicle.route.VehicleRoute
import java.util.UUID
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import io.scalac.seed.vehicle.domain.VehicleAggregate.{EmptyVehicle, Vehicle}
import org.json4s.{DefaultFormats, JObject}
import akka.util.Timeout

class VehicleRouteSpec extends FlatSpec with ScalatestRouteTest with Matchers with VehicleRoute {

  implicit val jsonFormats = DefaultFormats

  implicit val timeout = Timeout(2.seconds)

  def actorRefFactory = system

  val vehicleAggregateManager = system.actorOf(VehicleAggregateManager.props)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  "VehicleRoute" should "return not found if non-existing vehicle is requested" in {
    Get("/vehicles/" + UUID.randomUUID().toString) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.NotFound
    }
  }

  it should "create a vehicle" in {
    val regNumber = "123"
    val color = "Cerulean"
    Post("/vehicles", Map("regNumber" -> regNumber, "color" -> color)) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.Created
      val id = (responseAs[JObject] \ "id").extract[String]
      val vehicle = getVehicleFromManager(id)
      vehicle.regNumber shouldEqual regNumber
      vehicle.color shouldEqual color
    }
  }

  it should "return existing vehicle" in {
    val regNumber = "456"
    val color = "Navajo white"
    val vehicle = createVehicleInManager(regNumber, color)
    Get(s"/vehicles/" + vehicle.id) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val responseJson = responseAs[JObject]
      (responseJson \ "regNumber").extract[String] shouldEqual regNumber
      (responseJson \ "color").extract[String] shouldEqual color
    }
  }

  it should "remove vehicle" in {
    val vehicle = createVehicleInManager("123", "Pastel pink")
    Delete("/vehicles/" + vehicle.id) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.NoContent
      val emptyVehicleFuture = (vehicleAggregateManager ? GetVehicle(vehicle.id))
      val emptyVehicle = Await.result(emptyVehicleFuture, 2.seconds)
      emptyVehicle shouldBe EmptyVehicle
    }
  }

  it should "update vehicle's regNumber" in {
    val vehicle = createVehicleInManager("123", "Persian indigo")
    val newRegNumber = "456"
    Post(s"/vehicles/${vehicle.id}/regnumber", Map("value" -> newRegNumber)) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val updatedVehicle = getVehicleFromManager(vehicle.id)
      updatedVehicle.regNumber shouldEqual newRegNumber
    }
  }

  it should "update vehicle's color" in {
    val vehicle = createVehicleInManager("123", "Cherry blossom pink")
    val newColor = "Atomic tangerine"
    Post(s"/vehicles/${vehicle.id}/color", Map("value" -> newColor)) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val updatedVehicle = getVehicleFromManager(vehicle.id)
      updatedVehicle.color shouldEqual newColor
    }
  }

  private def getVehicleFromManager(id: String) = {
    val vehicleFuture = (vehicleAggregateManager ? GetVehicle(id)).mapTo[Vehicle]
    Await.result(vehicleFuture, 2.seconds)
  }

  private def createVehicleInManager(regNumber: String, color: String) = {
    val vehicleFuture = (vehicleAggregateManager ? RegisterVehicle(regNumber, color)).mapTo[Vehicle]
    Await.result(vehicleFuture, 2.seconds)
  }

}