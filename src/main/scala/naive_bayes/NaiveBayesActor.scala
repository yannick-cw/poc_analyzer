package naive_bayes

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor.{ClassificationResult, DocsToModel, ModelFinished, TestInput}
import utils.Model

/**
  * Created by Yannick on 23.05.16.
  */
object NaiveBayesActor {
  def props(master: ActorRef) = Props(new NaiveBayesActor(master))
  case class DocsToModel(docs: List[CleanedDoc])
  case class TestInput(algorithm: String, textList: String, originalText: String)
  case class ClassificationResult(repProb: Double, demProb: Double)
  case class ModelFinished(model: Model)
}

class NaiveBayesActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>

      println(s"using ${hits.size} docs for model")
      val (democrats, republican) = hits.partition(_._index == "dem")
      val getWords: (CleanedDoc => List[String]) = doc => doc.cleanedText.split(" ").toList
      val bayesAlgorithm = BayesAlgorithm(republican.map(_._source).map(getWords), democrats.map(_._source).map(getWords))
      println("own naive bayes is done")

      bayesAlgorithm.models.foreach{ model =>
        master ! ModelFinished(model)
      }
  }
}