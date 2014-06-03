package io.scalac.seed.route

import akka.actor._
import spray.httpx.Json4sSupport
import org.json4s.DefaultFormats
import spray.http.StatusCode
import spray.http.StatusCodes._
import akka.actor.SupervisorStrategy.Stop
import scala.concurrent.duration._
import io.scalac.seed.service.AggregateManager
import spray.routing.{HttpService, RequestContext}
import io.scalac.seed.domain.AggregateRoot
import scala.reflect.ClassTag
import io.scalac.seed.domain.AggregateRoot.{Removed, Uninitialized}
import io.scalac.seed.common.Error
import akka.actor.OneForOneStrategy

object PerRequest {

  case class RegisterRequestActor[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) extends PerRequest {
    override def processResult: Receive = {
      case tag(res) => complete(Created, res)
    }
  }

  case class UpdateRequestActor[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) extends PerRequest {
    override def processResult: Receive = {
      case tag(res) => complete(OK, res)
      case Uninitialized | Removed => complete(NotFound, "")
    }
  }

  case class DeleteRequestActor(r: RequestContext, target: ActorRef, message: AggregateManager.Command) extends PerRequest {
    override def processResult: Receive = {
      case Uninitialized => complete(NotFound, "")
      case Removed => complete(NoContent, "")
    }
  }

  case class GetRequestActor[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) extends PerRequest {
    override def processResult: Receive = {
      case tag(res) => complete(OK, res)
      case Uninitialized | Removed => complete(NotFound, "")
    }
  }

}

trait PerRequest extends Actor with ActorLogging with Json4sSupport {

  import context._

  val json4sFormats = DefaultFormats

  def r: RequestContext
  def target: ActorRef
  def message: AggregateManager.Command

  setReceiveTimeout(2.seconds)
  target ! message

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    r.complete(status, obj)
    stop(self)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        complete(InternalServerError, Error(e.getMessage))
        Stop
      }
    }

  def processResult: Receive

  override def receive = processResult orElse {
    case ReceiveTimeout =>
      complete(GatewayTimeout, Error("Request timeout"))
    case res =>
      log.error("received unexpected message " + res)
      complete(InternalServerError, "Something unexpected happened. We're working on it.")
  }

}

trait PerRequestCreator {
  self: HttpService =>

  import PerRequest._

  def perRequestRegister[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) =
    actorRefFactory.actorOf(Props(RegisterRequestActor[S](r, target, message)))

  def perRequestUpdate[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) =
    actorRefFactory.actorOf(Props(UpdateRequestActor[S](r, target, message)))

  def perRequestDelete(r: RequestContext, target: ActorRef, message: AggregateManager.Command) =
    actorRefFactory.actorOf(Props(DeleteRequestActor(r, target, message)))

  def perRequestGet[S <: AggregateRoot.State](r: RequestContext, target: ActorRef, message: AggregateManager.Command)(implicit tag: ClassTag[S]) =
    actorRefFactory.actorOf(Props(GetRequestActor[S](r, target, message)))

}