package io.scalac.seed.domain

import akka.actor._
import akka.persistence._
import io.scalac.seed.domain.VenueAggregate._

object VenueAggregate {

  object Rejected

  trait SeatState {
    def isFree: Boolean
    def takenBy(bookingId: String): Boolean
  }
  case object Free extends SeatState {
    def isFree: Boolean = return true
    def takenBy(bookingId: String): Boolean = return false
  }
  case class Taken(bookingId: String) extends SeatState {
    def isFree: Boolean = return false
    def takenBy(anotherBookingId: String): Boolean = return bookingId == anotherBookingId
  }

  trait VenueState
  case object Uninitialized extends VenueState
  case object Removed extends VenueState
  case class Venue(
    id: String,
    seatsTaken: scala.collection.immutable.Map[String, SeatState],
    bookings: scala.collection.immutable.Map[String, String]
  ) extends VenueState

  trait VenueCommand
  case class Initialize(seats: List[String]) extends VenueCommand
  case class Book(bookingId: String, seats: List[String]) extends VenueCommand
  case class UnbookAll(bookingId: String) extends VenueCommand
  case class KillAggregate() extends VenueCommand

  trait VenueEvent
  case class SeatsAdded(seats: List[String]) extends VenueEvent
  case class SeatsBooked(bookingId: String,   seats: List[String]) extends VenueEvent
  case class SeatsUnBooked(bookingId: String, seats: List[String]) extends VenueEvent

  def props(id: String): Props = Props(new VenueAggregate(id))
}

class VenueAggregate(id: String) extends PersistentActor with ActorLogging {
  protected var state: VenueState = VenueAggregate.Uninitialized

  def updateState(event: VenueEvent): Unit = {
    state match {
      case Uninitialized =>
        event match {
          case SeatsAdded(seats) =>
            val seatsTaken = new scala.collection.mutable.HashMap[String, SeatState]
            seats.foreach(s => seatsTaken(s) = Free)
            state = VenueAggregate.Venue(
              id,
              collection.immutable.HashMap(seatsTaken.toSeq: _*),
              new scala.collection.immutable.HashMap[String, String]
            )
        }
      case Venue(_, seatsTaken, bookings) =>
        event match{
          case SeatsBooked(bookingId, seats) =>
            state = Venue(
              id,
              seatsTaken ++ seats.map(s => (s -> Taken(bookingId))),
              bookings
            )
          case SeatsUnBooked(bookingId, seats) =>
            state = Venue(
              id,
              seatsTaken ++ seats.map(s => (s -> Free)),
              bookings
            )
        }
    }
  }

  def receiveCommand: Receive = {
    case Initialize(seats) =>
      state match {
        case Uninitialized =>
          persist(SeatsAdded(seats)){ event =>
            updateState(event)
            sender ! state
          }
        case _ => //reject
      }
    case Book(bookingId, seats) =>
      state match {
        case Venue(_,seatsTaken,_) => {
          if (seats.exists(s => !seatsTaken(s).isFree)) {
            sender ! Rejected
          } else {
            persist(SeatsBooked(bookingId = bookingId, seats = seats)){ event =>
              updateState(event)
              sender ! state
            }
          }
        }
      }
    case UnbookAll(bookingId) =>
      state match {
        case Venue(_,seatsTaken,_) => {
          val seats = seatsTaken.filter(kv => kv._2.takenBy(bookingId)).map(_._1).toList
          if (seats.isEmpty) {
            sender ! Rejected
          } else {
            persist(SeatsUnBooked(bookingId = bookingId, seats = seats)) { event =>
              updateState(event)
              sender ! state
            }
          }
        }
      }
  }

  override val receiveRecover: Receive = {
    case evt: VenueAggregate.VenueEvent =>
      updateState(evt)
  }

  override def persistenceId = id
}