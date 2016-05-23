package naive_bayes

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor.{ClassificationResult, DocsToModel, ModelFinished, TestInput}

/**
  * Created by Yannick on 23.05.16.
  */
object NaiveBayesActor {
  def props(master: ActorRef) = Props(new NaiveBayesActor(master))
  case class DocsToModel(docs: List[CleanedDoc])
  case class TestInput(textList: List[String])
  case class ClassificationResult(repProb: Double, demProb: Double)
  case object ModelFinished
}

class NaiveBayesActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>
      val (democrats, republican) = hits.partition(_._index == "dem")
      val getWords: (CleanedDoc => List[String]) = doc => doc.cleanedText.split(" ").toList

      val model = BayesModel(republican.map(_._source).map(getWords), democrats.map(_._source).map(getWords))

      master ! ModelFinished
      context become waitingForTestData(model)
  }

  def waitingForTestData(model: BayesModel): Receive = {
    case TestInput(textList) =>
      val classificationList =  model.classify(textList)
      master ! ClassificationResult(classificationList.head, classificationList.tail.head)
  }

}