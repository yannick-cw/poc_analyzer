package rest_connection

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.Hit
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.{BayesModelFinished, ClassificationResult, TestInput}
import rest_connection.MasterActor.ValidateAlgoRoute
import scala.util.Random.shuffle

/**
  * Created by Yannick on 23.05.16.
  */
object MasterActor {
  val props = Props(new MasterActor())
  val name = "master"

  case class ValidateAlgoRoute(testData: Int)
}

class MasterActor extends Actor {
  val elasticLoader = context.actorOf(LoadActor.props(self))
  val bayesActor = context.actorOf(NaiveBayesActor.props(self))

  def receive: Receive = {
    case start@StartImport(_, _) => elasticLoader ! start
      context become waitingForElasticData

    case ValidateAlgoRoute(testData) => elasticLoader ! StartImport()
      context become verifyingAlgo(testData, List.empty[Hit], List.empty[(String, String, Boolean)], null)
  }

  def waitingForElasticData: Receive = {
    case finishedImport: FinishedImport => bayesActor ! finishedImport
    case BayesModelFinished => context become acceptingTestData(self)
  }

  def verifyingAlgo(testDataPercentage: Int, testData: List[Hit], result: List[(String, String, Boolean)], lastElement: Hit): Receive = {
    case finishedImport: FinishedImport =>
      val (test, train) = shuffle(finishedImport.hits.filter(_._source.ups >= 10)).splitAt(finishedImport.hits.size * testDataPercentage / 100)
      bayesActor ! FinishedImport("","", train)
      context become verifyingAlgo(testDataPercentage, test, List.empty[(String, String, Boolean)], null)

    case BayesModelFinished =>
      bayesActor ! TestInput(testData.head._source.cleanedText.split(" +").toList)
      context become verifyingAlgo(testDataPercentage, testData.tail, result, testData.head)

    case ClassificationResult(rep, dem) =>
      if(testData.nonEmpty) {
        bayesActor ! TestInput(testData.head._source.cleanedText.split(" +").toList)
        context become verifyingAlgo(testDataPercentage, testData.tail, (lastElement._index, lastElement._source.cleanedText, (rep >= dem && lastElement._index == "rep") || (dem > rep && lastElement._index == "dem")) :: result, testData.head)
      } else {
        val resFalseTrue = result.map(_._3).groupBy(identity).mapValues(_.size)
        println(resFalseTrue)
        println("correct classified: " + (resFalseTrue(true).toDouble / (resFalseTrue(true).toDouble + resFalseTrue(false).toDouble)) * 100 + "%")
        val demRepFalse = result.groupBy(_._1).mapValues(_.map(_._3).count(_ == false))
        println(s"wrong dems: ${demRepFalse("dem")} and wrong reps: ${demRepFalse("rep")}")
      }
  }

  def acceptingTestData(requester: ActorRef): Receive = {
    case testInput: TestInput =>
      bayesActor ! testInput
      context become acceptingTestData(sender)
    case res@ClassificationResult(a,b) =>
      println(s"rep: $a, dem: $b")
      requester ! res
  }

}