import akka.actor.{Actor, Props}
import akka.event.Logging

object ClientActor {

  def props(id: String) = Props(new ClientActor(id))
}

class ClientActor(id: String) extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case "test" ⇒ log.info("received test")
    case _      ⇒ log.info("received unknown message")
  }
}
