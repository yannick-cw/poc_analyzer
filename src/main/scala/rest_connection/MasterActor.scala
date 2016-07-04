package rest_connection

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}
import tf_idf.TfIdfHelper
import utils.Model
import weka_bag_of_words.WekaBagOfWordsActor

import scala.util.Random._

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

  def receive: Receive = {
    case start@StartImport(_, _) => elasticLoader ! start
      context become working(Map.empty)
  }

  val minUpvotes: Int = 20


  def working(models: Map[String, Model]): Receive = {
    case FinishedImport(index, docType, hits) =>
      val minUpvotes: Int = 20
      println(s"allowing docs with min $minUpvotes upvotes")
      val (dem, rep) = hits
        .filter(_._source.ups >= minUpvotes)
        .partition(_._index == "dem")

      val allData = dem.zip(rep).flatten(tuple => List(tuple._1, tuple._2))
      println(s"using ${allData.size} docs total")
//        TfIdfHelper.updateData(hits)
      val finishedImport: FinishedImport = FinishedImport(index, docType, allData)
      bayesActor ! finishedImport
      wekaActor ! finishedImport

    case ModelFinished(model) => context become working(models.updated(model.name, model))

    case testInput@TestInput(algorithm, text, originalText) =>
        println(testInput)
      sender ! models.get(algorithm).map { model =>
        val classRes = model.classify(CleanedDoc("", 0, originalText, text))
        ClassificationResult(classRes.head, classRes.tail.head)
      }.getOrElse(ClassificationResult(0, 0))
  }
}
