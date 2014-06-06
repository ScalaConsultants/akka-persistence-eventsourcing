package io.scalac.seed.route

import spray.http.{BasicHttpCredentials, StatusCodes}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import spray.testkit.ScalatestRouteTest
import io.scalac.seed.service.{UserAggregateManager, VehicleAggregateManager}
import VehicleAggregateManager.{GetVehicle, RegisterVehicle}
import java.util.UUID
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import io.scalac.seed.domain.VehicleAggregate
import VehicleAggregate.Vehicle
import org.json4s.{DefaultFormats, JObject}
import akka.util.Timeout
import io.scalac.seed.domain.AggregateRoot.Removed
import io.scalac.seed.route.VehicleRoute
import io.scalac.seed.service.UserAggregateManager.RegisterUser
import scala.language.postfixOps

class VehicleRouteSpec extends FlatSpec with ScalatestRouteTest with Matchers with VehicleRoute with BeforeAndAfterAll {

  implicit val json4sFormats = DefaultFormats

  implicit val timeout = Timeout(2.seconds)

  def actorRefFactory = system

  val vehicleAggregateManager = system.actorOf(VehicleAggregateManager.props)

  val userAggregateManager = system.actorOf(UserAggregateManager.props)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  val credentials = BasicHttpCredentials("test", "test")

  override def beforeAll: Unit = {
    val userFuture = userAggregateManager ? RegisterUser("test", "test")
    Await.result(userFuture, 5 seconds)
  }

  "VehicleRoute" should "return not found if non-existing vehicle is requested" in {
    Get("/vehicles/" + UUID.randomUUID().toString) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.NotFound
    }
  }

  it should "create a vehicle" in {
    val regNumber = "123"
    val color = "Cerulean"
    Post("/vehicles", Map("regNumber" -> regNumber, "color" -> color)) ~> addCredentials(credentials) ~> vehicleRoute ~> check {
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
    Delete("/vehicles/" + vehicle.id) ~> addCredentials(credentials) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.NoContent
      val emptyVehicleFuture = (vehicleAggregateManager ? GetVehicle(vehicle.id))
      val emptyVehicle = Await.result(emptyVehicleFuture, 2.seconds)
      emptyVehicle shouldBe Removed
    }
  }

  it should "update vehicle's regNumber" in {
    val vehicle = createVehicleInManager("123", "Persian indigo")
    val newRegNumber = "456"
    Post(s"/vehicles/${vehicle.id}/regnumber", Map("value" -> newRegNumber)) ~> addCredentials(credentials) ~> vehicleRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val updatedVehicle = getVehicleFromManager(vehicle.id)
      updatedVehicle.regNumber shouldEqual newRegNumber
    }
  }

  it should "update vehicle's color" in {
    val vehicle = createVehicleInManager("123", "Cherry blossom pink")
    val newColor = "Atomic tangerine"
    Post(s"/vehicles/${vehicle.id}/color", Map("value" -> newColor)) ~> addCredentials(credentials) ~> vehicleRoute ~> check {
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