package rest_connection

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.Timeout
import elasicsearch_loader.LoadActor.StartImport
import util.{HttpRequester, Protocols, Settings}
import spray.json._
import akka.pattern.ask
import naive_bayes.NaiveBayesActor.{ClassificationResult, TestInput}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class ClassifyRequest(algorithm: String, text: String)
case class RawText(text: String)
case class CleanedText(cleanedText: List[String])

trait Service extends Protocols with HttpRequester {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val settings: Settings
  val master: ActorRef

  val classify = path("classify") {
      (post & entity(as[ClassifyRequest])) { request =>

        val rawText = RawText(request.text)
        val req = RequestBuilding.Post("/clean", entity = HttpEntity(ContentTypes.`application/json`, rawText.toJson.compactPrint))
        val futureCleaningRes = futureHttpResponse(req ,settings.cleaning.host, settings.cleaning.port)

        val classifyResult = for {
          cleanedText <- futureCleaningRes
          testInput <- Unmarshal(cleanedText).to[CleanedText].map(ct => TestInput(ct.cleanedText))
          classResult <- master.ask(testInput)(2 seconds)
        } yield classResult

        classifyResult.mapTo[ClassificationResult].foreach{ res =>
          println(s"rep: ${res.repProb}, dem: ${res.demProb}")
        }

        classifyResult.onFailure{
          case ex => ex.printStackTrace()
        }

        implicit val timeout = Timeout(5.seconds)
        complete("Wow")
      }
    }
}


object AkkaHttpMicroservice extends App with Service {
  implicit val system = ActorSystem("classify-system")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(system)
  val master = system.actorOf(MasterActor.props)
  master ! StartImport()
//  master ! ValidateAlgoRoute(10)
  Http().bindAndHandle(classify, "0.0.0.0", 9675)
}
