package rest_connection

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import elasicsearch_loader.LoadActor.StartImport
import naive_bayes.NaiveBayesActor.{ClassificationResult, TestInput}
import rest_connection.VerificationActor.ValidateAlgoRoute
import spray.json._
import util.{HttpRequester, Protocols, Settings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class ClassifyRequest(algorithm: String, text: String)
case class ClassifyResult(algorithm: String, rep: Double, dem: Double)
case class RawText(text: String)
case class CleanedText(cleanedText: List[String])

trait Service extends Protocols with HttpRequester {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val settings: Settings
  val master: ActorRef

  val classify = path("classify") {
      (post & entity(as[ClassifyRequest])) { request =>

        implicit val timeout = Timeout(5.seconds)

        println(request.text)
        println(request.algorithm)
        val rawText = RawText(request.text)
        val req = RequestBuilding.Post("/clean", entity = HttpEntity(ContentTypes.`application/json`, rawText.toJson.compactPrint))
        val futureCleaningRes = futureHttpResponse(req ,settings.cleaning.host, settings.cleaning.port)

        val classifyResult = for {
          cleanedText <- futureCleaningRes
          testInput <- Unmarshal(cleanedText).to[CleanedText].map(ct => TestInput(request.algorithm, ct.cleanedText))
          classResult <- master.ask(testInput)(4 seconds)
        } yield classResult


        complete {
          classifyResult.map[ToResponseMarshallable] {
            case ClassificationResult(rep, dem) => ClassifyResult(request.algorithm, rep, dem).toJson
          }
        }

      }
    }
}


object AkkaHttpMicroservice extends App with Service {
  implicit val system = ActorSystem("classify-system")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(system)
  val master = system.actorOf(MasterActor.props)
  val verify = system.actorOf(VerificationActor.props)
//  master ! StartImport()
  verify ! ValidateAlgoRoute("bayes_idf", 5)
  Http().bindAndHandle(classify, "0.0.0.0", 9675)
}
