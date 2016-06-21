package rest_connection

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.{BayesModel, NaiveBayesActor}
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}
import tf_idf.TfIdfActor
import utils.Model
import wekaTests.{WekaActor, WekaModel}

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
  val tfIdfActor = context.actorOf(TfIdfActor.props(self))
  val weka = context.actorOf(WekaActor.props(self))

  def receive: Receive = {
    case start@StartImport(_, _) => elasticLoader ! start
      context become working(Map.empty)
  }

  def working(models: Map[String, Model]): Receive = {
    case finishedImport: FinishedImport =>
      bayesActor ! finishedImport
    //      tfIdfActor ! finishedImport
    //      weka ! finishedImport
    case ModelFinished(model) => context become working(models.updated(model.name, model))

    case testInput@TestInput(algorithm, text, originalText) =>
      sender ! models.get(algorithm).map { model =>
        val classRes = model.classify(CleanedDoc("", 0, originalText, text.mkString(" ")))
        ClassificationResult(classRes.head, classRes.tail.head)
      }.getOrElse(ClassificationResult(0, 0))
  }
}
