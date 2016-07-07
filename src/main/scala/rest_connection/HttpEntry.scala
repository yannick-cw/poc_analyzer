package rest_connection

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
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
import utils.{HttpRequester, Protocols, Settings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class ClassifyRequest(algorithm: String, text: String)
case class ClassifyBulk(algorithm: String, texts: List[String])
case class ClassifyResult(algorithm: String, rep: Double, dem: Double)
case class BulkResult(results : List[ClassifyResult])
case class RawText(text: String)
case class BulkRaw(text: List[String])
case class CleanedText(cleanedText: String)
case class CleanedBulk(cleanedText: List[String])

trait Service extends Protocols with HttpRequester {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val settings: Settings
  val master: ActorRef
  val logger: LoggingAdapter

  val classify =
    path("classify") {
      (post & entity(as[ClassifyRequest])) { request =>

        implicit val timeout = Timeout(5.seconds)

        logger.debug(s"got single classify request for ${request.algorithm}")
        logger.debug(s"with raw text: ${request.text}")
        val rawText = RawText(request.text)
        val req = RequestBuilding.Post("/clean", entity = HttpEntity(ContentTypes.`application/json`, rawText.toJson.compactPrint))
        val futureCleaningRes = futureHttpResponse(req ,settings.cleaning.host, settings.cleaning.port)

        val classifyResult = for {
          cleanedText <- futureCleaningRes
          testInput <- Unmarshal(cleanedText).to[CleanedText].map(ct => TestInput(request.algorithm, ct.cleanedText, request.text))
          classResult <- master.ask(testInput)(4 seconds)
        } yield classResult


        complete {
          classifyResult.map[ToResponseMarshallable] {
            case ClassificationResult(rep, dem) =>
              logger.debug(s"classified with ${request.algorithm}, " + "rep:" + rep + ", dem:" + dem)
              ClassifyResult(request.algorithm, rep, dem).toJson
          }
        }

      }
    } ~
  path("classifyBulk") {
    (post & entity(as[ClassifyBulk])) { request =>

      implicit val timeout = Timeout(5.seconds)

      logger.debug(s"got twitter classify request for ${request.texts.size} posts.")
      val bulkRaw = BulkRaw(request.texts)
      val req = RequestBuilding.Post("/cleanBulk", entity = HttpEntity(ContentTypes.`application/json`, bulkRaw.toJson.compactPrint))
      val futureCleaningRes = futureHttpResponse(req ,settings.cleaning.host, settings.cleaning.port)

      val futureTestInputs = for {
        cleanedTexts <- futureCleaningRes
        testInputs <- Unmarshal(cleanedTexts).to[CleanedBulk].map(cts => cts.cleanedText.map(ct => TestInput(request.algorithm, ct, "")))
      } yield testInputs

      val futureClassifyResults = futureTestInputs
        .flatMap(testInputs => Future.sequence(testInputs
          .map(testInput => master.ask(testInput)(4 seconds)
          .mapTo[ClassificationResult])))

      val futureBulkRes = futureClassifyResults.map(res => BulkResult(res.map(cRes => ClassifyResult(request.algorithm, cRes.repProb, cRes.demProb))))
      futureBulkRes.onSuccess{
        case res => logger.debug(s"classified twitter account with:  ${res.results.mkString(" ")} ")
      }

      complete(futureBulkRes)
      }
  }
}


object AkkaHttpMicroservice extends App with Service {
  implicit val system = ActorSystem("classify-system")
  implicit val materializer = ActorMaterializer()
  val logger = Logging.getLogger(system, this)

  val settings = Settings(system)
  val master = system.actorOf(MasterActor.props)
  val verify = system.actorOf(VerificationActor.props)
  master ! StartImport()
//  verify ! ValidateAlgoRoute("bayes", 5)
  Http().bindAndHandle(classify, "0.0.0.0", 9675)
}
