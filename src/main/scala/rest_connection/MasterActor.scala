package rest_connection

import akka.actor.{Actor, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}
import utils.Model
import weka_bag_of_words.WekaBagOfWordsActor

/**
  * Created by Yannick on 23.05.16.
  */
object MasterActor {
  val props = Props(new MasterActor())
  val name = "master"
}

class MasterActor extends Actor {
  val elasticLoader = context.actorOf(LoadActor.props(self))
  val bayesActor = context.actorOf(NaiveBayesActor.props(self))
  val wekaActor = context.actorOf(WekaBagOfWordsActor.props(self))

  val minUpvotes: Int = 20

  def receive: Receive = {
    case start@StartImport(_, _) => elasticLoader ! start
      context become working(Map.empty)
  }

  def working(models: Map[String, Model]): Receive = {
    case FinishedImport(index, docType, hits) =>
      println(s"allowing docs with min $minUpvotes upvotes")
      val filteredHits = FinishedImport(index, docType, hits.filter(_._source.ups > minUpvotes))
      println(s"using ${filteredHits.hits.size} docs total")
      bayesActor ! filteredHits
      wekaActor ! filteredHits

    case ModelFinished(model) => context become working(models.updated(model.name, model))

    case testInput@TestInput(algorithm, text, originalText) =>
      sender ! models.get(algorithm).map { model =>
        val classRes = model.classify(CleanedDoc("", 0, originalText, text))
        ClassificationResult(classRes.head, classRes.tail.head)
      }.getOrElse(ClassificationResult(0, 0))
  }
}
