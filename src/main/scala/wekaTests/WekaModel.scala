package wekaTests

import java.util

import utils.Model
import elasicsearch_loader.Queries._
import weka.classifiers.bayes.NaiveBayes
import weka.core.{Attribute, DenseInstance, Instances}


/**
  * Created by Yannick on 14/06/16.
  */
class WekaModel(hits: List[Hit]) extends FeatureBuilder with Model {
  val name = "weka"

  val mistakes = new Attribute("mistakes")
  val sentenceLength = new Attribute("sentenceLength")
  val words = new Attribute("words")
  val upper = new Attribute("upper")
  val wordLength = new Attribute("wordLength")
  val distinctWords = new Attribute("distinctWords")

  val classVec = new util.ArrayList[String](2)
  classVec.add("rep")
  classVec.add("dem")
  val classAttr = new Attribute("class", classVec)

  val fvAttr = new util.ArrayList[Attribute](7)
  fvAttr.add(mistakes)
  fvAttr.add(sentenceLength)
  fvAttr.add(words)
  fvAttr.add(upper)
  fvAttr.add(wordLength)
  fvAttr.add(distinctWords)
  fvAttr.add(classAttr)

  val trainSet = new Instances("train", fvAttr, 10000)
  trainSet.setClassIndex(6)

  hits.par.foreach { hit =>
    val train = new DenseInstance(7)
    val rawText = hit._source.rawText
    train.setValue(fvAttr.get(0), mistakesPerWord(rawText))
    train.setValue(fvAttr.get(1), normalizedSentenceLength(rawText))
    train.setValue(fvAttr.get(2), wordsInDoc(rawText))
    train.setValue(fvAttr.get(3), normalizedUppercaseLetters(rawText))
    train.setValue(fvAttr.get(4), normalizedWordLength(rawText))
    train.setValue(fvAttr.get(5), normalizedDistinctWords(rawText))
    train.setValue(fvAttr.get(6), hit._index)
    trainSet.add(train)
  }

  val classifyModel = new NaiveBayes()
  classifyModel.buildClassifier(trainSet)

  def classify(cleanedDoc: CleanedDoc) = {
    val orgString = cleanedDoc.rawText
    val testSet = new Instances("test", fvAttr, 10)
    testSet.setClassIndex(6)
    val test = new DenseInstance(7)

    println(mistakesPerWord(orgString))
    println(normalizedSentenceLength(orgString))
    println(wordsInDoc(orgString))
    println(normalizedUppercaseLetters(orgString))
    println(normalizedWordLength(orgString))
    println(normalizedDistinctWords(orgString))
    println(orgString)
    println()
    println()

    test.setValue(fvAttr.get(0), mistakesPerWord(orgString))
    test.setValue(fvAttr.get(1), normalizedSentenceLength(orgString))
    test.setValue(fvAttr.get(2), wordsInDoc(orgString))
    test.setValue(fvAttr.get(3), normalizedUppercaseLetters(orgString))
    test.setValue(fvAttr.get(4), normalizedWordLength(orgString))
    test.setValue(fvAttr.get(5), normalizedDistinctWords(orgString))
    testSet.add(test)

    val dis = classifyModel.distributionForInstance(testSet.instance(0))
    dis
  }
}
