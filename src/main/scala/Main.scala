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

  val source = Source(1 to 100)

  val done = source.runForeach(i ⇒ println(i))(materializer)

  implicit val ec = system.dispatcher
  done.onComplete(_ ⇒ system.terminate())
}
