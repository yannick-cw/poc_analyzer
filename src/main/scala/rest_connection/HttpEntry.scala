package rest_connection

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import util.Protocols

import scala.concurrent.duration._

case class ClassifyRequest(algorithm: String, text: String)

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  val classify = path("classify") {
      (post & entity(as[ClassifyRequest])) { request =>

        implicit val timeout = Timeout(2.seconds)
        complete("Wow")
      }
    }
}


object AkkaHttpMicroservice extends App with Service {
  implicit val system = ActorSystem("classify-system")
  implicit val materializer = ActorMaterializer()

  Http().bindAndHandle(classify, "0.0.0.0", 9675)
}
