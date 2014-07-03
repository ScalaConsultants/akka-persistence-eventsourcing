package io.scalac.seed.route

import akka.actor._
import io.scalac.seed.domain.UserAggregate
import io.scalac.seed.service._
import spray.httpx.Json4sSupport
import spray.routing._
import spray.routing.authentication.BasicAuth

object UserRoute {
  case class ChangePasswordRequest(pass: String)
}

trait UserRoute extends HttpService with Json4sSupport with RequestHandlerCreator with UserAuthenticator {

  import UserAggregateManager._

  val userAggregateManager: ActorRef
  
  val userRoute =
    pathPrefix("user") {
      pathEndOrSingleSlash {
        post {
          entity(as[RegisterUser]) { cmd =>
            serveRegister(cmd)
          }
        }
      } ~
      path("password") {
        post {
          authenticate(BasicAuth(userAuthenticator _, realm = "secure site")) { user =>
            entity(as[UserRoute.ChangePasswordRequest]) { cmd =>
              serveUpdate(ChangeUserPassword(user.id, cmd.pass))
            }
          }
        }
      }
    }

  private def serveRegister(message : AggregateManager.Command): Route =
    ctx => handleRegister[UserAggregate.User](ctx, userAggregateManager, message)

  private def serveUpdate(message : AggregateManager.Command): Route =
    ctx => handleUpdate[UserAggregate.User](ctx, userAggregateManager, message)

}