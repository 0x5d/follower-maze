import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.ByteString

object ClientActor {

  final case class SetNotifier(a: ActorRef)
  final case class SetId(id: String)

  object Message {

    // This could be better w/ Shapeless
    def apply(msg: String): Message = {
      msg.split("\\|").toList match {
        case id :: "F" :: from :: to :: Nil => Follow(msg, id, from, to)
        case id :: "U" :: from :: to :: Nil => Unfollow(msg, id, from, to)
        case id :: "P" :: from :: to :: Nil => Private(msg, id, from, to)
        case id :: "S" :: from :: Nil => StatusUpdate(msg, id, from)
        case id :: "B" :: Nil => Broadcast(msg, id)
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
}

class ClientActor extends Actor {
  import ClientActor._

  var id = ""
  var notifier: Option[ActorRef] = None

  val log = Logging(context.system, this)

  def receive = {
    case SetId(s) => id = s
    case SetNotifier(a) => notifier = Some(a)

    case Follow(msg, msgId, from, to) if to == id => send(msg)
    case Unfollow(msg, msgId, from, to) if to == id => send(msg)
    case Private(msg, msgId, from, to) if to == id => send(msg)
    case StatusUpdate(msg, msgId, from) => send(msg)
    case Broadcast(msg, msgId) => send(msg)
    case UnknownMessage(msg) => log.info(s"Unknown: $msg")
    case _ => log.info("Not my problem")
  }

  def send(msg: String) = notifier.foreach(_ ! ByteString(msg))
}
