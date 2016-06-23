package wekaTests

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}

/**
  * Created by Yannick on 23.05.16.
  */
object WekaActor {
  def props(master: ActorRef) = Props(new WekaActor(master))
  case class DocsToModel(docs: List[CleanedDoc])
  case class TestInput(algorithm: String, textList: List[String])
  case class ClassificationResult(repProb: Double, demProb: Double)
  case object ModelFinished
}

class WekaActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>
      val model = new WekaModel(hits)
      println("model done")
      master ! ModelFinished(model)
  }
}