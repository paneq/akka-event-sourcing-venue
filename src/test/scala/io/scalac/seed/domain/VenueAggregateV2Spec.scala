import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import io.scalac.seed.domain.VenueAggregate
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

import io.scalac.seed.domain.VenueAggregate._

class VenueAggregateV2Spec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "can be initialized with list of seats" in {
    val id = UUID.randomUUID().toString()
    val venue = system.actorOf(Props(new VenueAggregate(id)))
    val initializeCommand = VenueAggregate.Initialize(seats = List("A-1", "A-2"))
    venue ! initializeCommand
    expectMsg(VenueAggregate.Venue(id = id, seatsTaken = Map("A-1"-> Free, "A-2" -> Free)))
  }

  "can book some seats" in {
    val id = UUID.randomUUID().toString()
    val venue = system.actorOf(Props(new VenueAggregate(id)))
    venue ! VenueAggregate.Initialize(seats = List("A-1", "A-2"))
    receiveN(1)
    venue ! VenueAggregate.Book(bookingId = "booking-1", seats = List("A-1", "A-2"))
    expectMsg(VenueAggregate.Venue(id = id, seatsTaken = Map("A-1"-> Taken("booking-1"), "A-2" -> Taken("booking-1"))))
  }

  "can unbook seats of a booking" in {
    val id = UUID.randomUUID().toString()
    val venue = system.actorOf(Props(new VenueAggregate(id)))
    venue ! Initialize(seats = List("A-1", "A-2"))
    receiveN(1)
    venue ! Book(bookingId = "booking-1", seats = List("A-1", "A-2"))
    receiveN(1)
    venue ! UnbookAll(bookingId = "booking-1")
    expectMsg(VenueAggregate.Venue(id = id, seatsTaken = Map("A-1"-> Free, "A-2" -> Free)))
  }
}