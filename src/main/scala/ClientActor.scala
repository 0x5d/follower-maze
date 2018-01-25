import akka.actor.{Actor, Props}
import akka.event.Logging

object ClientActor {

  object Message {

    // This could be better w/ Shapeless
    def apply(msg: String): Message = {
      msg.split("|").toList match {
        case id :: "F" :: from :: to :: Nil => Follow(msg, id, from, to)
        case id :: "U" :: from :: to :: Nil => Unfollow(msg, id, from, to)
        case id :: "P" :: from :: to :: Nil => Private(msg, id, from, to)
        case id :: "S" :: from :: Nil => StatusUpdate(msg, id, from)
        case id :: "B" :: from :: Nil => Broadcast(msg, id)
        case _ => UnknownMessage(msg)
      }
    }
  }

  trait Message {
    val original: String
    val id: String
  }
  final case class Follow(original: String, id: String, from: String, to: String) extends Message
  final case class Unfollow(original: String, id: String, from: String, to: String) extends Message
  final case class Private(original: String, id: String, from: String, to: String) extends Message
  final case class StatusUpdate(original: String, id: String, from: String) extends Message
  final case class Broadcast(original: String, id: String) extends Message
  final case class UnknownMessage(original: String) extends Message {
    val id = "unknown"
  }

  def props(id: String) = Props(new ClientActor(id))
}

class ClientActor(id: String) extends Actor {
  import ClientActor._

  val log = Logging(context.system, this)

  def receive = {
    case Follow(msg, msgId, from, to) if to == id => log.info(msg)
    case Unfollow(msg, msgId, from, to) if to == id => log.info(msg)
    case Private(msg, msgId, from, to) if to == id => log.info(msg)
    case StatusUpdate(msg, msgId, from) => log.info(msg)
    case Broadcast(msg, msgId) => log.info(msg)
    case _ ⇒ log.info("received unknown message")
  }
}
