package io.scalac.seed.service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import io.scalac.seed.domain.VehicleAggregate
import VehicleAggregate._
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.language.postfixOps

class VehicleAggregateManagerSpec extends FlatSpec with BeforeAndAfterAll {

  import VehicleAggregateManager._

  implicit val actorSystem = ActorSystem("vehicleAggregateManagerSpec-system")
  
  implicit val timeout = Timeout(2 seconds)

  implicit val executionContext = actorSystem.dispatcher
  
  override def afterAll = {
    actorSystem.shutdown
  }
  
  "VehicleAggregateManager" should "create new child actor when creating new vehicle" in {
    val manager = TestActorRef(VehicleAggregateManager.props, "VehicleAggregateManager-test-actor")
    
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

    val initialSize = manager.children.size
    
    //update the vehicle
    manager ! UpdateColor(id, "col2")
    
    //check children size
    val finalSize = manager.children.size
    
    assert(finalSize == initialSize)
  }

  it should "kill child actors when max count is exceeded" in {
    val manager = TestActorRef(VehicleAggregateManager.props)

    //create more vehicles than manager should keep
    implicit val timeout = Timeout(5 seconds)
    val futures = (0 to AggregateManager.maxChildren * 2).foldLeft(Seq[Future[Vehicle]]()) { (futures, _) =>
      futures :+ (manager ? RegisterVehicle(regNumber = "reg1", color = "col1")).mapTo[Vehicle]
    }

    val future = Future sequence futures
    Await.result(future, 5 seconds)

    val finalSize = manager.children.size

    assert(finalSize <= AggregateManager.maxChildren)
  }
  
}