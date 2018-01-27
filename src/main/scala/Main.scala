import ClientActor.{Message, SetId, SetNotifier}
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.{ActorSystem, Props}
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
          system.actorSelection("/user/**") ! m
        }.map( _ => ByteString("ok"))

      c.handleWith(preProcess.via(process))
    }

  // Clients

  val clients = Tcp()
    .bind(host, clientsPort)
    .runForeach { c ⇒

      val src = Source.actorRef[ByteString](1000, OverflowStrategy.fail)

      val clientActor = system.actorOf(Props(new ClientActor))

      val sink = Flow[ByteString]
        .via(preProcess)
        .via(Flow[String].map(id => clientActor ! SetId(id)))
        .to(Sink.ignore)

      val (notifier, _) = c.flow
        .join(BidiFlow.identity[ByteString, ByteString])
        .runWith(src, sink)

      clientActor ! SetNotifier(notifier)
    }
}
