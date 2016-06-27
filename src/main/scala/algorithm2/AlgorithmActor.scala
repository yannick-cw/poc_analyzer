package algorithm2

import akka.actor.{Actor, ActorRef, Props}
import algorithm2.AlgorithmActor.{BayesModelFinished, ClassificationResult, TestInput}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc


object AlgorithmActor {
  def props(master: ActorRef) = Props(new AlgorithmActor(master))
  case class DocsToModel(docs: List[CleanedDoc])
  case class TestInput(textList: List[String])
  case class ClassificationResult(repProb: Double, demProb: Double)
  case object BayesModelFinished
}

class AlgorithmActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>
      val (democrats, republican) = hits.partition(_._index == "dem")

      val getWords: (CleanedDoc => List[String]) = doc => doc.cleanedText.split(" ").toList

      val model = AlgorithmModel(republican.map(_._source).map(getWords), democrats.map(_._source).map(getWords))

      master ! BayesModelFinished
      context become waitingForTestData(model)
  }

  def waitingForTestData(model: AlgorithmModel): Receive = {
    case TestInput(textList) =>
      val classificationList =  model.classify(textList)
      master ! ClassificationResult(classificationList.head, classificationList.tail.head)
  }

}