package io.scalac.seed.route

import akka.actor._
import spray.httpx.Json4sSupport
import spray.routing._
import io.scalac.seed.service.{UserAggregateManager, AggregateManager}
import io.scalac.seed.domain.UserAggregate

trait UserRoute extends HttpService with Json4sSupport with RequestHandlerCreator {

  import UserAggregateManager._

  val userAggregateManager: ActorRef
  
  val userRoute =
    path("user") {
      post {
        entity(as[RegisterUser]) { cmd =>
          serveRegister(cmd)
        }
      }
    }

  private def serveRegister(message : AggregateManager.Command): Route =
    ctx => handleRegister[UserAggregate.User](ctx, userAggregateManager, message)

}