package tf_idf

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}

object TfIdfActor {
  def props(master: ActorRef) = Props(new TfIdfHelperActor(master))
}

class TfIdfActor(master: ActorRef) extends Actor {

  def receive = modelBuilding

  def modelBuilding: Receive = {
    case FinishedImport(_, _, hits) =>

      println(s"using ${hits.size} docs for tf*idf model")
      val (democrats, republican) = hits.partition(_._index == "dem")

      val getWords: (CleanedDoc => List[String]) = doc => doc.cleanedText.split(" ").toList

      val model = TfIdfModel(republican.map(_._source).map(getWords), democrats.map(_._source).map(getWords))
      println("TF*IDF model finished")

      master ! ModelFinished(model)
  }
}