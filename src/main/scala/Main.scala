import ClientActor.Message
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.ActorSystem
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
      // CONNECTIONS CAN'T BE SHARED (https://doc.akka.io/docs/akka/current/stream/stream-io.html), SO:
      // get a client incoming connection ->
      // create an actor to wait for a bit and get the events from the publisher corresponding to the current client ->
      // ask the actor for the received events ->
      // the actor blocks for a time window (shorter than the client timeout) ->
      // the actor returns the receiver events ->
      // send the events, sorted by event sequence id, to the client
      val actorCreation = Flow[String].map(id => system.actorOf(ClientActor.props(id), name = id))
      val process = Flow[String]
        .via(actorCreation)
        .map(_ => ByteString("ok"))

      c.handleWith(preProcess.via(process))
    }
}
