package io.scalac.seed.vehicle.service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success
import scala.concurrent.Await
import io.scalac.seed.vehicle.domain.VehicleAggregate._

class VehicleAggregateManagerSpec extends FlatSpec with BeforeAndAfterAll {

  import VehicleAggregateManager._
  
  implicit val actorSystem = ActorSystem("vehicleAggregateManagerSpec-system")
  
  implicit val timeout = Timeout(2 seconds)
  
  override def afterAll = {
    actorSystem.shutdown
  }
  
  "VehicleAggregateManager" should "create new child actor when creating new vehicle" in {
    val manager = TestActorRef(VehicleAggregateManager.props)
    
    val initialSize = manager.children.size
    
    manager ! RegisterVehicle(regNumber = "reg1", color = "col1")
    
    val finalSize = manager.children.size
    
    assert(finalSize == initialSize + 1)
  }
  
  it should "use existing child actor when updating vehicle data" in {
    val manager = TestActorRef(VehicleAggregateManager.props)

    //create a new vehicle
    val future = (manager ? RegisterVehicle(regNumber = "reg1", color = "col1")).mapTo[Vehicle]
    
    val Vehicle(id, _, _) = Await.result(future, 2 seconds)
    //val Success(id: String) = future.value.get
    
    val initialSize = manager.children.size
    
    //update the vehicle
    manager ! UpdateColor(id, "col2")
    
    //check children size
    val finalSize = manager.children.size
    
    assert(finalSize == initialSize)
  }
  
}