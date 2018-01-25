import ClientActor.Message
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.ActorSystem
import akka.stream.javadsl.Tcp.IncomingConnection
import akka.util.ByteString

object Main extends App {

  implicit val system = ActorSystem("follower-maze")
  implicit val materializer = ActorMaterializer()

  val host = "127.0.0.1"
  val sourcePort = 9090
  val clientsPort = 9099

  val delim = sys.props("line.separator")
  val maxFrameLength = 265
  val allowTruncation = true

  val framing = Framing.delimiter(
    ByteString(delim),
    maxFrameLength,
    allowTruncation
  )

  val preProcess = Flow[ByteString]
    .via(framing)
    .map(_.utf8String)

  // Publisher

  val src = Tcp()
    .bind(host, sourcePort)
    .runForeach { c ⇒
      val process = Flow[String]
        .map(Message(_))
        .map( msg => ByteString(msg.original))

      c.flow.via(preProcess).via(process)
    }

  // Clients

  val actorCreation = Flow[String].map(s => (s, system.actorOf(ClientActor.props(s))))

  val clients = Tcp()
    .bind(host, clientsPort)
    .runForeach { c ⇒
      val process = Flow[String]
        .via(actorCreation)
        .map { case (s, _) => ByteString(s)}

      c.flow.via(preProcess).via(process)
    }
}
