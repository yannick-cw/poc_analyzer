package elasicsearch_loader

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import elasicsearch_loader.LoadActor.StartImport
import elasicsearch_loader.Queries.{CleanedDoc, ScrollResponse}
import util.Settings

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

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
      val scrollId = Await.result(futureScrollId, 2 seconds)
      println(scrollId)

      def go(id: ScrollId, acc: List[CleanedDoc]): Future[List[CleanedDoc]] = {
        val scrollSet: Future[ScrollResponse] = getNextSet(id)
        scrollSet.flatMap { scroll =>
          val currentId = scroll._scroll_id
          val docs = scroll.hits.hits.map(_._source)
          if(docs.isEmpty) Future(acc)
          else go(currentId, acc ++ docs)
        }
      }

      val docs = go(scrollId, List.empty[CleanedDoc])
  }

}

object Tester extends App {
  val actor = ActorSystem().actorOf(LoadActor.props)
  actor ! StartImport()
}
