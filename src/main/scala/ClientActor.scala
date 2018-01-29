import scala.collection.mutable
import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.util.ByteString

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object ClientActor {

  final case class SetNotifier(a: ActorRef)
  final case class SetId(id: String)
  object Flush

  object Event {

    // This could be better w/ Shapeless
    def apply(msg: String): Event = {
      msg.split("\\|").toList match {
        case id :: "F" :: from :: to :: Nil => Follow(msg, id.toInt, from, to)
        case id :: "U" :: from :: to :: Nil => Unfollow(msg, id.toInt, from, to)
        case id :: "P" :: from :: to :: Nil => Private(msg, id.toInt, from, to)
        case id :: "S" :: from :: Nil => StatusUpdate(msg, id.toInt, from)
        case id :: "B" :: Nil => Broadcast(msg, id.toInt)
        case _ => UnknownEvent(msg)
      }
    }
  }

  trait Event {
    val original: String
    val id: Int
  }
  final case class Follow(original: String, id: Int, from: String, to: String) extends Event
  final case class Unfollow(original: String, id: Int, from: String, to: String) extends Event
  final case class Private(original: String, id: Int, from: String, to: String) extends Event
  final case class StatusUpdate(original: String, id: Int, from: String) extends Event
  final case class Broadcast(original: String, id: Int) extends Event
  final case class UnknownEvent(original: String) extends Event {
    val id = -1
  }
}

class ClientActor extends Actor {
  import ClientActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  private var id = ""
  private var notifier: Option[ActorRef] = None

  private val followees = mutable.Set[String]()
  private var events = Seq[Event]()

  val log = Logging(context.system, this)

  val interval = FiniteDuration(1, SECONDS)

  context.system.scheduler.schedule(interval, interval, self, Flush)

  def receive = {
    case Flush =>
      flush

    case SetId(s) => id = s

    case SetNotifier(a) => notifier = Some(a)

    case e: Follow =>
      if (e.from == id) followees += e.to
      else if (e.to == id) events = events :+ e

    case e: Unfollow =>
      if (e.from == id) followees -= e.to

    case e: Private if e.to == id => events = events :+ e

    case e: StatusUpdate if followees.contains(e.from) => events = events :+ e

    case e: Broadcast => events = events :+ e

    case UnknownEvent(msg) => log.info(s"Unknown: $msg")

    case _ => ()
  }

  private def send(msg: String) = notifier.foreach(_ ! ByteString(msg))

  private def flush = {
    if (events.nonEmpty) {
      println(s"Flushing $id: $events")
      val es = events.sortBy(_.id)
      events = Seq[Event]()
      es.foreach(e => send(e.original))
    }
  }
}
