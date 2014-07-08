package io.scalac.seed.route

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.pattern.ask
import akka.util.Timeout
import com.github.t3hnar.bcrypt._
import io.scalac.seed.domain.UserAggregate.User
import io.scalac.seed.service.UserAggregateManager
import io.scalac.seed.service.UserAggregateManager._
import org.json4s.DefaultFormats
import org.scalatest._
import spray.http._
import spray.testkit.ScalatestRouteTest

class UserRouteSpec extends FlatSpec with ScalatestRouteTest with Matchers with UserRoute with BeforeAndAfterAll {

  implicit val json4sFormats = DefaultFormats

  implicit val timeout = Timeout(2.seconds)

  implicit def executionContext = system.dispatcher
  
  def actorRefFactory = system

  val userAggregateManager = system.actorOf(UserAggregateManager.props)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  "UserRoute" should "register new user" in {
    val name = "test"
    val pass = "test"
    Post("/user", Map("name" -> name, "pass" -> pass)) ~> userRoute ~> check {
      response.status shouldBe StatusCodes.Created
      val user = getUserFromManager(name)

      //password should be encrypted
      pass.isBcrypted(user.pass) shouldBe true
    }
  }

  it should "update user's password" in {
    val name = "John"
    val pass = "test"
    val newPass = "s3cr3t"
    val user = createUserInManager(name, pass)

    val credentials = BasicHttpCredentials(name, pass)
    Post("/user/password", Map("pass" -> newPass)) ~> addCredentials(credentials) ~> userRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val user = getUserFromManager(name)

      //password should be changed and encrypted
      newPass.isBcrypted(user.pass) shouldBe true
    }
  }

  private def getUserFromManager(name: String) = {
    val userFuture = (userAggregateManager ? GetUser(name)).mapTo[User]
    Await.result(userFuture, 2.seconds)
  }

  private def createUserInManager(name: String, pass: String) = {
    val userFuture = (userAggregateManager ? RegisterUser(name, pass)).mapTo[User]
    Await.result(userFuture, 2.seconds)
  }

}