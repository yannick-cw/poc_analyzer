package rest_connection

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.scaladsl.Source
import elasicsearch_loader.LoadActor
import elasicsearch_loader.LoadActor.{FinishedImport, StartImport}
import elasicsearch_loader.Queries.Hit
import naive_bayes.NaiveBayesActor
import naive_bayes.NaiveBayesActor.ModelFinished
import rest_connection.VerificationActor.{EvalResult, ValidateAlgoRoute}
import tf_idf.TfIdfActor
import wekaTests.{FeatureBuilder, WekaActor}

import scala.util.Random._

/**
  * Created by Yannick on 23.05.16.
  */
object VerificationActor {
  val props = Props(new VerificationActor())
  val name = "verification"

  case class ValidateAlgoRoute(algorithm: String, testData: Int)
  case class EvalResult(expected: String, originalMessage: String, correct: Boolean)
}

class VerificationActor extends Actor with FeatureBuilder {
  val elasticLoader = context.actorOf(LoadActor.props(self))
  val bayesActor = context.actorOf(NaiveBayesActor.props(self))
  val tfIdfActor = context.actorOf(TfIdfActor.props(self))
  val wekaActor = context.actorOf(WekaActor.props(self))

  def receive: Receive = {
    case ValidateAlgoRoute(algo, testDataPercent) => elasticLoader ! StartImport()
      algo match {
        case "bayes" => context become createModel(bayesActor, testDataPercent)
        case "bayes_idf" => context become createModel(tfIdfActor, testDataPercent)
        case "weka" => context become createModel(wekaActor, testDataPercent)
      }
  }

  def createModel(algoActor: ActorRef, testDataPercentage: Int): Receive = {
    case finishedImport: FinishedImport =>
      val minUpvotes: Int = 1
      println(s"allowing docs with min $minUpvotes upvotes")
      val (dem, rep) = shuffle(finishedImport.hits)
          .filter(_._source.ups >= minUpvotes)
          .partition(_._index == "dem")

      val allData = dem.zip(rep).flatten(tuple => List(tuple._1, tuple._2)).take(10000)
      println(s"using ${allData.size} docs total")

      val (test, train) = allData.splitAt(allData.size * testDataPercentage / 100)
      println(s"using train data ${train.size}")
      println(s"using test data ${test.size}")

      algoActor ! FinishedImport("", "", train)
      context become evaluating(test)
  }

  def evaluating(testData: List[Hit]): Receive = {
    case ModelFinished(model) =>
          val res = testData.map{ hit =>
              val eval = model.classify(hit._source)
              val evaluated: String = if (eval.head >= eval.tail.head) "rep" else "dem"
              EvalResult(
                expected = hit._index,
                originalMessage = hit._source.cleanedText,
                correct = hit._index == evaluated)
            }

          val correctClassified = res.filter(_.correct)
          val correctClassifiedReps = correctClassified.filter(_.expected == "rep")
          val correctClassifiedDems = correctClassified.filter(_.expected == "dem")
          val wrongClassifiedDems = res.filterNot(_.correct).filter(_.expected == "dem")
          val wrongClassifiedReps = res.filterNot(_.correct).filter(_.expected == "rep")
          val percentageCorrect = (correctClassified.size.toDouble / res.size.toDouble) * 100

          println(s"percentage correct: $percentageCorrect")
          println(s"wrong dems: ${wrongClassifiedDems.size}")
          println(s"wrong reps: ${wrongClassifiedReps.size}")
          println(s"correct dems: ${correctClassifiedDems.size}")
          println(s"correct reps: ${correctClassifiedReps.size}")
  }
}

