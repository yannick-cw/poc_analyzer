package tf_idf

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import tf_idf.TfIdfActor.{ClassificationResult, ModelFinished, TestInput}

object TfIdfActor {
  def props(master: ActorRef) = Props(new TfIdfActor(master))
  case class DocsToModel(docs: List[CleanedDoc])
  case class TestInput(algorithm: String, textList: List[String])
  case class ClassificationResult(repProb: Double, demProb: Double)
  case object ModelFinished
}

class TfIdfActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>

      println(s"using ${hits.size} docs for tf*idf model")
      val (democrats, republican) = hits.partition(_._index == "dem")

      val getWords: (CleanedDoc => List[String]) = doc => doc.cleanedText.split(" ").toList

      val model = TfIdfModel(republican.map(_._source).map(getWords), democrats.map(_._source).map(getWords))

      master ! ModelFinished
      println("TF*IDF model finished")
      context become waitingForTestData(model)
  }

  def waitingForTestData(model: TfIdfModel): Receive = {
    case TestInput(_, textList) =>
        print(s"tf*idf received $textList")
      val classificationList =  model.classify(textList)
      master ! ClassificationResult(classificationList.head, classificationList.tail.head)
  }

}