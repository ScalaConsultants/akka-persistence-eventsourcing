package io.scalac.seed

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can._

object Boot extends App {

  implicit val system = ActorSystem("seed-actor-system")
  
  implicit val executionContext = system.dispatcher

  val service = system.actorOf(Props(new ServiceActor), "seed-service")

  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)
}