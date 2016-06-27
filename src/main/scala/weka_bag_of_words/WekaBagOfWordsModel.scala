package weka_bag_of_words

import java.util

import elasicsearch_loader.Queries._
import utils.Model
import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.meta.FilteredClassifier
import weka.core.{Attribute, DenseInstance, Instances}
import weka.filters.unsupervised.attribute.StringToWordVector
/**
  * Created by Yannick on 14/06/16.
  */
class WekaBagOfWordsModel(hits: List[Hit]) extends Model {
  val name = "weka_bag_of_words"

  val textAttr = new Attribute("text", null :util.List[String])
  val classVec = new util.ArrayList[String](2)
  classVec.add("rep")
  classVec.add("dem")
  val classAttr = new Attribute("repOrDem", classVec)

  val fvAttr = new util.ArrayList[Attribute](2)
  fvAttr.add(classAttr)
  fvAttr.add(textAttr)

  val trainData = new Instances("train", fvAttr, 400000)
  trainData.setClassIndex(0)

  hits.filter(_._source.cleanedText.nonEmpty).foreach { hit =>
    val train = new DenseInstance(2)
    train.setValue(fvAttr.get(0), hit._index)
    train.setValue(fvAttr.get(1), hit._source.cleanedText)
    trainData.add(train)
  }

  println("instance is done")

  val filter = new StringToWordVector()
  filter.setStemmer(null)
  filter.setAttributeIndices("last")
  filter.setIDFTransform(true)
  filter.setStopwordsHandler(null)
  val classifier = new FilteredClassifier()
  classifier.setFilter(filter)
  classifier.setClassifier(new NaiveBayes())
  classifier.buildClassifier(trainData)

  def classify(cleanedDoc: CleanedDoc) = {
    val cleanedString = cleanedDoc.cleanedText
    val testData = new Instances("test", fvAttr, 1)
    testData.setClassIndex(0)
    val test = new DenseInstance(2)

    test.setValue(fvAttr.get(1), cleanedString)
    testData.add(test)

    classifier.distributionForInstance(testData.instance(0))
  }
}
