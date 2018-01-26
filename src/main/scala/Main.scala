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
        .map { m =>
          println(m)
          system.actorSelection("/user/**") ! m
        }.map( _ => ByteString("ok"))

      c.handleWith(preProcess.via(process))

//      c.flow.via(preProcess).via(process).toMat(Sink.ignore)(Keep.right)
    }

  // Clients

  val clients = Tcp()
    .bind(host, clientsPort)
    .runForeach { c ⇒
      val actorCreation = Flow[String].map(id => (id, system.actorOf(ClientActor.props(id, c), name = id)))
      val process = Flow[String]
        .via(actorCreation)
        .map(_ => ByteString("ok"))

      c.handleWith(preProcess.via(process))
    }
}
