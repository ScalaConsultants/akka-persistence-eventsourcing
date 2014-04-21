package io.scalac.seed

import akka.actor._
import akka.actor.SupervisorStrategy.Stop
import akka.actor.OneForOneStrategy
import io.scalac.seed.common._
import org.json4s.DefaultFormats
import scala.concurrent.duration._
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.routing.RequestContext
import spray.httpx.Json4sSupport

trait PerRequest extends Actor with Json4sSupport {

  import context._

  val json4sFormats = DefaultFormats

  def r: RequestContext
  def target: ActorRef
  def message: RestMessage

  setReceiveTimeout(2.seconds)
  target ! message

  def receive = {
    case res: AnyRef => complete(OK, res)
    //case v: Validation    => complete(BadRequest, v)
    case ReceiveTimeout   => complete(GatewayTimeout, Error("Request timeout"))
  }

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
}

object PerRequest {
  case class WithActorRef(r: RequestContext, target: ActorRef, message: RestMessage) extends PerRequest

  case class WithProps(r: RequestContext, props: Props, message: RestMessage) extends PerRequest {
    lazy val target = context.actorOf(props)
  }
}

trait PerRequestCreator {
  this: Actor =>

  import PerRequest._
  
  def perRequest(r: RequestContext, target: ActorRef, message: RestMessage) =
    context.actorOf(Props(new WithActorRef(r, target, message)))

  def perRequest(r: RequestContext, props: Props, message: RestMessage) =
    context.actorOf(Props(new WithProps(r, props, message)))
}