package rest_connection

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.{BayesModel, NaiveBayesActor}
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}
import tf_idf.{TfIdfHelper}
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
    val weka = context.actorOf(WekaActor.props(self))


    def receive: Receive = {
        case start@StartImport(_, _) => elasticLoader ! start
            context become working(Map.empty)
    }


    def working(models: Map[String, Model]): Receive = {
        case finishedImport: FinishedImport =>
            TfIdfHelper.updateData(finishedImport.hits)
            bayesActor ! finishedImport
        //      weka ! finishedImport
        case ModelFinished(model) => context become working(models.updated(model.name, model))

        case testInput@TestInput(algorithm, text, originalText) =>
            val (baseAlgorithm,useTfIdf) = algorithm.endsWith("_tfidf") match {
                case true => (algorithm.split("_tfidf").head, true)
                case false => (algorithm, false)
            }
            sender ! models.get(baseAlgorithm).map { model =>
                val classRes = model.classify(CleanedDoc("", 0, originalText, text.mkString(" ")), useTfIdf)
                ClassificationResult(classRes.head, classRes.tail.head)
            }.getOrElse(ClassificationResult(0, 0))
    }
}
