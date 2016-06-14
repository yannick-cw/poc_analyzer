package rest_connection

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.Hit
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.{ClassificationResult, ModelFinished, TestInput}
import rest_connection.VerificationActor.ValidateAlgoRoute
import tf_idf.TfIdfActor
import wekaTests.WekaTest

import scala.util.Random._

/**
  * Created by Yannick on 23.05.16.
  */
object VerificationActor {
  val props = Props(new VerificationActor())
  val name = "verification"

  case class ValidateAlgoRoute(algorithm: String, testData: Int)
}

class VerificationActor extends Actor {
  val elasticLoader = context.actorOf(LoadActor.props(self))
  val bayesActor = context.actorOf(NaiveBayesActor.props(self))
  val tfIdfActor = context.actorOf(TfIdfActor.props(self))
  val wekaActor = context.actorOf(WekaTest.props(self))

  def receive: Receive = {
    case ValidateAlgoRoute(algo, testData) => elasticLoader ! StartImport()
      algo match {
        case "bayes" => context become verifyingAlgo(bayesActor, testData, List.empty[Hit], List.empty[(String, String, Boolean)], null)
        case "bayes_idf" => context become verifyingAlgo(tfIdfActor, testData, List.empty[Hit], List.empty[(String, String, Boolean)], null)
        case "weka" => context become verifyingAlgo(wekaActor, testData, List.empty[Hit], List.empty[(String, String, Boolean)], null)
      }
  }

  def verifyingAlgo(algoActor: ActorRef, testDataPercentage: Int, testData: List[Hit], result: List[(String, String, Boolean)], lastElement: Hit): Receive = {
    case finishedImport: FinishedImport =>
      val minUpvotes: Int = 20
      println(s"allowing docs with min $minUpvotes upvotes")
      val filterByMinUp = finishedImport.hits.filter(_._source.ups >= minUpvotes)
      val (testRaw, trainRaw) = shuffle(filterByMinUp).splitAt(filterByMinUp.size * testDataPercentage / 100)
      val (d,r) = trainRaw.partition(_._index == "dem")
      val (td, tr) = testRaw.partition(_._index == "dem")
      val maxItems = scala.math.min(d.size, r.size)
      val maxItemsTest = scala.math.min(td.size, tr.size)
      val train = d.take(maxItems) ++ r.take(maxItems)
      val test = td.take(maxItemsTest) ++ tr.take(maxItemsTest)
      println(s"using train data ${train.size}")
      println(s"using test data ${test.size}")

      algoActor ! FinishedImport("", "", train)
      context become verifyingAlgo(algoActor, testDataPercentage, test, List.empty[(String, String, Boolean)], null)

    case ModelFinished =>
      algoActor ! TestInput("", testData.head._source.cleanedText.split(" +").toList, testData.head._source.rawText)
      context become verifyingAlgo(algoActor, testDataPercentage, testData.tail, result, testData.head)

    case ClassificationResult(rep, dem) =>
      if (testData.nonEmpty) {
        algoActor ! TestInput("", testData.head._source.cleanedText.split(" +").toList, testData.head._source.rawText)
        context become verifyingAlgo(algoActor, testDataPercentage, testData.tail, (lastElement._index, lastElement._source.cleanedText, (rep >= dem && lastElement._index == "rep") || (dem > rep && lastElement._index == "dem")) :: result, testData.head)
      } else {
        val resFalseTrue = result.map(_._3).groupBy(identity).mapValues(_.size)
        println(resFalseTrue)
        println("correct classified: " + (resFalseTrue(true).toDouble / (resFalseTrue(true).toDouble + resFalseTrue(false).toDouble)) * 100 + "%")
        val demRepFalse = result.groupBy(_._1).mapValues(_.map(_._3).count(_ == false))
        println(s"wrong dems: ${demRepFalse("dem")} and wrong reps: ${demRepFalse("rep")}")
      }
  }
}

