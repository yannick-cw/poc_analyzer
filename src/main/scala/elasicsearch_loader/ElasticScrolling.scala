package elasicsearch_loader

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import elasicsearch_loader.Queries.ScrollResponse
import util.{HttpRequester, Protocols, Settings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Yannick on 23.05.16.
  */
trait ElasticScrolling extends HttpRequester with Protocols {
  val settings: Settings
  type ScrollId = String

  def initScrollScan(index: Option[String] = None, docType: Option[String] = None, scrollWindow: Int = 10000): Future[ScrollId] = {
    val queryIndex = index.map(i => s"/$i")
    val queryDocType = docType.map(d => s"/$d")

    val req = RequestBuilding.Get(s"${queryIndex.getOrElse("")}${queryDocType.getOrElse("")}/_search?search_type=scan&scroll=1m&size=$scrollWindow")
    val futureRes = futureHttpResponse(req, settings.elasti.host, settings.elasti.port)

    val scrollFuture = futureRes.flatMap{
      case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[ScrollResponse]
      case HttpResponse(code, _, entity, _) => Future.failed(IllegalResponseException(s"status: $code"))
    }

    scrollFuture.map(_._scroll_id)
  }

  def getNextSet(scrollId: ScrollId): Future[ScrollResponse] = {
    val data = s"""{"scroll": "1m","scroll_id": "$scrollId"}"""
    val request = RequestBuilding.Get(s"/_search/scroll/", entity = HttpEntity(ContentTypes.`application/json`, data))
    val futureRes = futureHttpResponse(request, settings.elasti.host, settings.elasti.port)
    futureRes.flatMap{
      case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[ScrollResponse]
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes.runForeach(bytes => println(bytes.decodeString("UTF-8")))
        Future.failed(IllegalResponseException(s"status: $code"))
    }
  }

}
