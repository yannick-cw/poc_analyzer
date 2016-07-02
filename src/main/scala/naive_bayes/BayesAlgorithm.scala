package naive_bayes

import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.BayesAlgorithm.{Class, Word}
import tf_idf.TfIdfHelper
import utils.Model

/**
  * Created by Yannick on 23.05.16.
  */
object BayesAlgorithm {
  type Class = List[Doc]
  type Doc = List[Word]
  type Word = String

  def apply(classes: Class*): BayesAlgorithm = new BayesAlgorithm(classes: _*)
}

class BayesAlgorithm(classes: Class*) {
  //require(classes.forall(_.nonEmpty))
  val minWordAppearance: Int = 0
  println(
    s"allowing words with min $minWordAppearance word appearance in class")

  //number of docs in class divided by number of all docs
  private val probabilityPerClass =
    classes.map(_class => _class.size.toDouble / classes.flatten.size.toDouble)

  private val vocabularySize = classes.flatten.flatten.distinct.size.toDouble
  println("vocabulary " + vocabularySize)

  private val wordsPerClass = classes.map(_.flatten.size)
  println("words per class " + wordsPerClass)

  private val getPerWordCount: (Class) => Map[Word, Double] = _class => {
    _class.flatten
      .groupBy(identity)
      .mapValues(_.length.toDouble)
      .filter(_._2 >= minWordAppearance)
  }

  // danger of out of memory here, maybe .par
  private val perClassWordAppearance = classes.map(getPerWordCount)
  println(s"model has ${
    perClassWordAppearance.head.size +
      perClassWordAppearance.tail.head.size
  } distinct words in both rep and dem")
  println("done with model")
  val zipped = wordsPerClass.zip(perClassWordAppearance)

  val models = List(
    new Model {
      override  def classify(cleanedDoc: CleanedDoc): Seq[Double] = {
        val inputText = cleanedDoc.cleanedText.split(" ")
        val zipped = wordsPerClass.zip(perClassWordAppearance)

        val classWiseProbabilities = zipped
          .map { case (totalWordsClass, individualWordCountMap) => inputText
            .map{ word => (individualWordCountMap.getOrElse(word, 0.0) + 1.0) / (totalWordsClass + vocabularySize)}
          }

        classWiseProbabilities
          .map(_.product)
          .zip(probabilityPerClass)
          .map{ case (wordInClassProbability, generalClasProbability) =>  wordInClassProbability * generalClasProbability }
      }
      override val name: String = "naive_bayes"
    },
    new Model {
      override def classify(cleanedDoc: CleanedDoc): Seq[Double] = ???
//      {
//        val rawInputTextSplit = cleanedDoc.cleanedText.split(" ")
//
//        val tfIdfs = useTfIdf match {
//          case true => Some(TfIdfHelper.calculate(rawInputTextSplit).resultTfIdfs)
//          case false => None
//        }
//
//        val inputText = useTfIdf match {
//          case true => rawInputTextSplit.distinct
//          case false => rawInputTextSplit
//        }
//
//        val classWiseProbabilities = zipped.map {
//          case (totalWordsClass, individualWordCountMap) => {
//
//            inputText.map { word => {
//              val tfIdfForWord = tfIdfs.isDefined match {
//                case true => tfIdfs.get.getOrElse(word, 1.0)
//                case false => 1.0
//              }
//
//              tfIdfForWord * (individualWordCountMap.getOrElse(word, 0.0) +
//                1.0) / (totalWordsClass + vocabularySize)
//            }
//            }
//          }
//        }
//
//        classWiseProbabilities.map(_.product).zip(probabilityPerClass).map {
//          case (wordInClassProbability, generalClasProbability) =>
//            wordInClassProbability * generalClasProbability
//        }
//      }
      override val name: String = "naive_bayes_tfidf"
    }
  )
}
