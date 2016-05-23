package elasicsearch_loader

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import elasicsearch_loader.LoadActor.StartImport
import elasicsearch_loader.Queries.{CleanedDoc, ScrollResponse}
import util.Settings

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Yannick on 23.05.16.
  */
object LoadActor{
  val props = Props(new LoadActor())
  val name = "loade-actor"
  case class StartImport(index: Option[String] = None, docType: Option[String] = None)
}

class LoadActor extends Actor with ElasticScrolling {

  private val sys: ActorSystem = context.system
  override val settings: Settings = Settings(sys)
  override implicit val system: ActorSystem = sys
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive: Receive = {
    case StartImport(index, docType) =>
      val futureScrollId = initScrollScan(index, docType)
      val scrollId = Await.result(futureScrollId, 10 seconds)

      @tailrec
      def go(id: ScrollId, acc: List[CleanedDoc]): List[CleanedDoc] = {
        val result: ScrollResponse = Await.result(getNextSet(id), 10 seconds)
        val newDocs = result.hits.hits.map(_._source)
        if (newDocs.isEmpty) acc
        else go(result._scroll_id, acc ++ newDocs)
      }

      val docs = go(scrollId, List.empty[CleanedDoc])

      docs.foreach(println)
  }

}
