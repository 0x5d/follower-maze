import akka.stream._
import akka.stream.scaladsl._
import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object Main extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val host = "127.0.0.1"
  val sourcePort = 9090
  val clientsPort = 9099

  Tcp()
    .bind(host, sourcePort)
    .runForeach { c ⇒
      val echo = Flow[ByteString]
        .via(Framing.delimiter(
          ByteString("\n"),
          maximumFrameLength = 256,
          allowTruncation = true))
        .map(_.utf8String)
        .map(ByteString(_))

      c.handleWith(echo)
    }

  Tcp()
    .bind(host, clientsPort)
    .runForeach { c ⇒
      println(s"New connection: ${c.remoteAddress}")
    }
}
