import akka.stream._
import akka.stream.scaladsl._
import akka.actor.ActorSystem
import akka.util.ByteString

object Main extends App {

  implicit val system = ActorSystem("follower-maze")
  implicit val materializer = ActorMaterializer()

  // This is how I can create an actor.
  // Now I just need to do it for every new client.
  system.actorOf(ClientActor.props("someId"))

  val host = "127.0.0.1"
  val sourcePort = 9090
  val clientsPort = 9099

  val delim = sys.props("line.separator")
  val maxFrameLength = 265
  val allowTruncation = true

  val src = Tcp()
    .bind(host, sourcePort)
    .runForeach { c ⇒
      val rcv = Flow[ByteString]
        .via(Framing.delimiter(
          ByteString(delim),
          maxFrameLength,
          allowTruncation
        ))
        .map(_.utf8String)
        .map(ByteString(_))

      c.handleWith(rcv)
    }

  val clients = Tcp()
    .bind(host, clientsPort)
    .runForeach { c ⇒
      val rcv = Flow[ByteString]
        .via(Framing.delimiter(
          ByteString(delim),
          maxFrameLength,
          allowTruncation
        ))
        .map(_.utf8String)
        .map(ByteString(_))

      c.handleWith(rcv)
    }
}
