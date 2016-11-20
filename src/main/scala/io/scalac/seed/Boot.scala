package io.scalac.seed

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import io.scalac.seed.domain.VenueAggregate
import spray.can._

object Boot extends App {

  implicit val system = ActorSystem("seed-actor-system")

  implicit val executionContext = system.dispatcher
//
//  val service = system.actorOf(Props(new ServiceActor), "seed-service")
//
//  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)

  val venue = system.actorOf(VenueAggregate.props("1"), "venue1")
  venue ! VenueAggregate.Initialize( List("A-1", "A-2", "B-1", "B-2") )
}