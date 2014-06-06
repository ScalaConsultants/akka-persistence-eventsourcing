package io.scalac.seed.route

import akka.actor.ActorRef
import spray.routing.authentication.UserPass
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import io.scalac.seed.service.UserAggregateManager.GetUser
import io.scalac.seed.domain.UserAggregate.User
import com.github.t3hnar.bcrypt._

trait UserAuthenticator {

  val userAggregateManager: ActorRef

  def userAuthenticator(userPass: Option[UserPass]): Future[Option[User]] =
    userPass match {
      case Some(UserPass(user, pass)) =>
        implicit val timeout = Timeout(2 seconds)
        (userAggregateManager ? GetUser(user)).map( _ match {
          case u: User if pass.isBcrypted(u.pass) => Some(u)
          case _ => None
        })
      case None =>
        Future(None)
    }

}
