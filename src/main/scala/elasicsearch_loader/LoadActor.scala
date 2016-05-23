package elasicsearch_loader

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.{CleanedDoc, Hit, ScrollResponse}
import util.Settings

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Yannick on 23.05.16.
  */
object LoadActor{
  def props(master: ActorRef) = Props(new LoadActor(master))
  val name = "load-actor"
  case class StartImport(index: Option[String] = None, docType: Option[String] = None)
  case class FinishedImport(index: String, docType: String, hits: List[Hit])
}

class LoadActor(master: ActorRef) extends Actor with ElasticScrolling {

  private val sys: ActorSystem = context.system
  override val settings: Settings = Settings(sys)
  override implicit val system: ActorSystem = sys
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive: Receive = {
    case StartImport(index, docType) =>
      val futureScrollId = initScrollScan(index, docType)
      val scrollId = Await.result(futureScrollId, 2 seconds)

      def go(id: ScrollId, acc: List[Hit]): Future[List[Hit]] = {
        val scrollSet: Future[ScrollResponse] = getNextSet(id)
        scrollSet.flatMap { scroll =>
          val currentId = scroll._scroll_id
          val hits = scroll.hits.hits
          if(hits.isEmpty) Future.successful(acc)
          else go(currentId, acc ++ hits)
        }
      }

      val futureDocs = go(scrollId, List.empty[Hit])

      futureDocs.onSuccess{
        case hits =>
          println(s"imported ${hits.size} docs")
          master ! FinishedImport(index.getOrElse("all"), docType.getOrElse("all"), hits)
      }

      futureDocs.onFailure{
        case ex => ex.printStackTrace()
      }
  }

}
