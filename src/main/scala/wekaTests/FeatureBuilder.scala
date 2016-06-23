package wekaTests

import naive_bayes.BayesModel.Doc

import scala.io.Source

/**
  * Created by Yannick on 14/06/16.
  */
trait FeatureBuilder {
  private lazy val dictionary = extractWords(Source.fromInputStream(getClass.getResourceAsStream("/big.txt")).mkString.toLowerCase)

  def mistakesPerWord(text: String): Double = {
    val doc = extractWords(text) map (_.toLowerCase)
    if(doc.nonEmpty) doc.map(dictionary.contains).count(!_).toDouble / doc.size else 0
  }

  def normalizedSentenceLength(text: String): Double = if(text.nonEmpty) {
    val sentences = text.split("[.!?:;]")
    sentences.map(extractWords(_).size).sum.toDouble / sentences.size
  } else 0


  def wordsInDoc(text: String): Int = extractWords(text).size

  def normalizedUppercaseLetters(text: String): Double = if(text.nonEmpty) {
    val words: Doc = extractWords(text)
    words.map(_.count(c => c.toUpper == c)).sum.toDouble / words.size.toDouble
  } else 0

  def normalizedWordLength(text: String): Double = if(text.nonEmpty) {
    val words: Doc = extractWords(text)
    words.map(_.size).sum.toDouble / words.size
  } else 0

  def normalizedDistinctWords(text: String): Double = if(text.nonEmpty) {
    val words: Doc = extractWords(text)
    words.distinct.size.toDouble / words.size.toDouble
  } else 0


  private def extractWords(text: String): Doc = s"[a-zA-Z]+".r.findAllIn(text).toList
}
