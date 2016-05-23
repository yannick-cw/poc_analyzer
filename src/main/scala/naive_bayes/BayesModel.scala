package naive_bayes

import naive_bayes.BayesModel.Class

/**
  * Created by Yannick on 23.05.16.
  */

object BayesModel {
  type Class = List[Doc]
  type Doc = List[String]

  def apply(classes: Class*): BayesModel = new BayesModel(classes:_*)
}

class BayesModel(classes: Class*) {
  require(classes.forall(_.nonEmpty))

  val probabilityPerClass = classes.map(doc => doc.size.toDouble / classes.flatten.size.toDouble)
  val vocabularySize = classes.flatten.flatten.distinct.size.toDouble
  val perClassWordAppearance = classes.map(c => (c.flatten.size ,c.flatten.groupBy(s => s).map(tuple => (tuple._1, tuple._2.size.toDouble))))

  def classify(inputText: List[String]): Seq[Double] = {

    val classWise = perClassWordAppearance.map { m => inputText.map{word => (m._2.getOrElse(word, 0.0) + 1.0) / (m._1 + vocabularySize)} }
    classWise.map(_.product).zip(probabilityPerClass).map(tuple => tuple._1 * tuple._2)
  }
}
