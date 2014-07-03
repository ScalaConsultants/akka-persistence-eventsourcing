package io.scalac.seed.route

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.t3hnar.bcrypt._
import io.scalac.seed.domain.UserAggregate.User
import io.scalac.seed.service.UserAggregateManager.GetUser
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.routing.authentication.UserPass

trait UserAuthenticator {

  val userAggregateManager: ActorRef

  implicit def executionContext: ExecutionContext
  
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
