package rest_connection

import akka.actor.{Actor, ActorSystem, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}

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

  def receive: Receive = {
    case start@StartImport(_, _) => elasticLoader ! start
      context become waitingForElasticData
  }

  def waitingForElasticData: Receive = {
    case finishedImport: FinishedImport => bayesActor ! finishedImport
    case ModelFinished => context become acceptingTestData
  }

  def acceptingTestData: Receive = {
    case testInput: TestInput => bayesActor ! testInput
    case ClassificationResult(a,b) => println(s"rep: $a, dem: $b")
  }

}

object Test extends App {
  val system = ActorSystem()
  system.actorOf(MasterActor.props) ! StartImport()
}
