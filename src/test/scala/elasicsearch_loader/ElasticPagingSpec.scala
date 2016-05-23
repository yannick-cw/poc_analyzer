package elasicsearch_loader

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}
import util.{Settings, TestData}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Yannick on 23.05.16.
  */
class ElasticPagingSpec extends TestKit(ActorSystem("testSys")) with WordSpecLike with MustMatchers {
  implicit val materializer = ActorMaterializer()
  val testSys = system
  val testMat = materializer

  "ElasticPaging" must {
    "find the given scroll Id" in {
      val testData = TestData.initIdResponse.getBytes

      val elasticPaging = new ElasticScrolling {
        override val settings: Settings = Settings(testSys)
        override implicit val materializer: ActorMaterializer = testMat
        override implicit val system: ActorSystem = testSys

        override def futureHttpResponse(req: HttpRequest, host: String, port: Int): Future[HttpResponse] = {
          Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, testData)))
        }
      }

      val initScan: Future[String] = elasticPaging.initScrollScan()
      val result = Await.result(initScan, 1 seconds)
      result must be(TestData.initId)
    }

    "extract all the cleanedDocs from a nextSet query" in {

      val testData = TestData.scrollResponse.getBytes

      val elasticPaging = new ElasticScrolling {
        override val settings: Settings = Settings(testSys)
        override implicit val materializer: ActorMaterializer = testMat
        override implicit val system: ActorSystem = testSys

        override def futureHttpResponse(req: HttpRequest, host: String, port: Int): Future[HttpResponse] = {
          Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, testData)))
        }
      }

      val futureRes = elasticPaging.getNextSet("does not matter here")
      val result = Await.result(futureRes, 1 seconds)
      result.hits.total must be(180964)
      result.hits.hits.map(_._source).toSet must be(TestData.hits)
    }

    "return error if the response code did not match" in {
      val elasticPaging = new ElasticScrolling {
        override val settings: Settings = Settings(testSys)
        override implicit val materializer: ActorMaterializer = testMat
        override implicit val system: ActorSystem = testSys

        override def futureHttpResponse(req: HttpRequest, host: String, port: Int): Future[HttpResponse] = {
          Future.successful(HttpResponse(status = StatusCodes.BadRequest, entity = HttpEntity(ContentTypes.`application/json`, "")))
        }
      }

      import scala.concurrent.ExecutionContext.Implicits.global
      val initScan: Future[String] = elasticPaging.initScrollScan()
      initScan.onFailure{
        case ex => ex.getMessage must be(s"status: ${StatusCodes.BadRequest}")
      }
    }

    "return error if the connection to elasticsearch failed" in {
      val elasticPaging = new ElasticScrolling {
        override val settings: Settings = Settings(testSys)
        override implicit val materializer: ActorMaterializer = testMat
        override implicit val system: ActorSystem = testSys

        override def futureHttpResponse(req: HttpRequest, host: String, port: Int): Future[HttpResponse] = {
          Future.failed(IllegalHeaderException("wuups"))
        }
      }

      import scala.concurrent.ExecutionContext.Implicits.global
      val initScan: Future[String] = elasticPaging.initScrollScan()
      initScan.onFailure{
        case ex => ex.getMessage must be("wuups")
      }
    }
  }

}
