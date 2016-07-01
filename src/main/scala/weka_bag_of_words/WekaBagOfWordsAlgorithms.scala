package weka_bag_of_words

import java.util

import elasicsearch_loader.Queries._
import utils.Model
import weka.classifiers.Classifier
import weka.classifiers.`lazy`.IBk
import weka.classifiers.bayes.{BayesNet, NaiveBayesMultinomialText}
import weka.classifiers.meta.FilteredClassifier
import weka.core.{Attribute, DenseInstance, Instances}
import weka.filters.unsupervised.attribute.StringToWordVector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * Created by Yannick on 14/06/16.
  */
class WekaBagOfWordsAlgorithms(hits: List[Hit]) {

  private val textAttr = new Attribute("text", null :util.List[String])
  private val classVec = new util.ArrayList[String](2)
  classVec.add("rep")
  classVec.add("dem")

  private val classAttr = new Attribute("repOrDem", classVec)

  private val fvAttr = new util.ArrayList[Attribute](2)
  fvAttr.add(classAttr)
  fvAttr.add(textAttr)

  private val trainData = new Instances("train", fvAttr, 400000)
  trainData.setClassIndex(0)

  hits.filter(_._source.cleanedText.nonEmpty).foreach { hit =>
    val train = new DenseInstance(2)
    train.setValue(fvAttr.get(0), hit._index)
    train.setValue(fvAttr.get(1), hit._source.cleanedText)
    trainData.add(train)
  }

  println("instance is done")

  def stringToWordFilter: StringToWordVector = {
    val filter = new StringToWordVector()
    filter.setStemmer(null)
    filter.setAttributeIndices("last")
    filter.setIDFTransform(true)
    filter.setStopwordsHandler(null)
    filter
  }

  def createFilterClassifier(algorithm: Classifier): FilteredClassifier = {
    val classifier = new FilteredClassifier()
    classifier.setFilter(stringToWordFilter)
    classifier.setClassifier(algorithm)
    classifier.buildClassifier(trainData)
    classifier
  }

  lazy val models: List[Future[Model]] = List(
    Future( new Model {
      val classifier = createFilterClassifier(new BayesNet())
      override def classify(inputText: CleanedDoc, useTfIdf: Boolean = false): Seq[Double] = classifyWithClassifier(inputText, classifier)
      override val name: String = "bayes_net"
    }),
    Future( new Model {
      val classifier = createFilterClassifier(new IBk())
      override def classify(inputText: CleanedDoc, useTfIdf: Boolean = false): Seq[Double] = classifyWithClassifier(inputText, classifier)
      override val name: String = "knn"
    }),
    Future( new Model {
      private val classifier = new NaiveBayesMultinomialText()
      classifier.buildClassifier(trainData)
      override def classify(inputText: CleanedDoc, useTfIdf: Boolean = false): Seq[Double] = classifyWithClassifier(inputText, classifier)
      override val name: String = "bayes_multinomial"
    })
  )

  def classifyWithClassifier(cleanedDoc: CleanedDoc, classifier: Classifier) = {
    val cleanedString = cleanedDoc.cleanedText
    val testData = new Instances("test", fvAttr, 1)
    testData.setClassIndex(0)
    val test = new DenseInstance(2)

    test.setValue(fvAttr.get(1), cleanedString)
    testData.add(test)

    classifier.distributionForInstance(testData.instance(0))
  }
}
