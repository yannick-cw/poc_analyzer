package algorithm2

import naive_bayes.BayesAlgorithm.{Class, Word}

/**
  * Created by Yannick on 23.05.16.
  */

object AlgorithmModel {
  type Class = List[Doc]
  type Doc = List[Word]
  type Word = String

  def apply(classes: Class*): AlgorithmModel = new AlgorithmModel(classes:_*)
}

class AlgorithmModel(classes: Class*) {
  require(classes.forall(_.nonEmpty))

  //number of docs in class divided by number of all docs
  private val probabilityPerClass = classes.map(_class => _class.size.toDouble / classes.flatten.size.toDouble)

  private val vocabularySize = classes.flatten.flatten.distinct.size.toDouble
  println("vocabulary " + vocabularySize)

  private val wordsPerClass = classes.map(_.flatten.size)
  println("words per class " + wordsPerClass)

  private val getPerWordCount: (Class) => Map[Word, Double] = _class =>
    _class
    .flatten
    .groupBy(identity)
    .mapValues(_.length.toDouble)

  // danger of out of memory here, possibly filter with appearance less than 2
  private val perClassWordAppearance = classes.map(getPerWordCount)
  println("done with model")

  def classify(inputText: List[Word]): Seq[Double] = {

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
}
