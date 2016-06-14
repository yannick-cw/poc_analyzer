package tf_idf

import tf_idf.TfIdfModel.{Class, Word, Doc}

object TfIdfModel {
  type Class = List[Doc]
  type Doc = List[Word]
  type Word = String

  def apply(classes: Class*): TfIdfModel = new TfIdfModel(classes: _*)
}

class TfIdfModel(classes: Class*) {
  //require(classes.forall(_.nonEmpty))
  val minWordAppearance: Int = 0
  println(s"allowing words with min $minWordAppearance word appearance in class")

  private val documents = classes.flatten
  private val numberOfDocuments = documents.size.toDouble

  //number of docs in class divided by number of all docs
  private val probabilityPerClass = classes.map(_class => _class.size.toDouble / documents.size.toDouble)

  private val vocabularySize = documents.flatten.distinct.size.toDouble
  println("vocabulary " + vocabularySize)

  private val wordsPerClass = classes.map(_.flatten.size)
  println("words per class " + wordsPerClass)

  private val getPerWordCount: (Class) => Map[Word, Double] = _class => {
    _class
      .flatten
      .groupBy(identity)
      .mapValues(_.length.toDouble)
      .filter(_._2 >= minWordAppearance)
  }

  // danger of out of memory here, maybe .par
  private val perClassWordAppearance = classes.map(getPerWordCount)
  println(s"model has ${perClassWordAppearance.head.size + perClassWordAppearance.tail.head.size} distinct words in both rep and dem")
  println("done with model")


  private var idfForWords: Map[Word, Double] = Map.empty

  //TODO calculate idf only on words contained in user input text
  def calculateIDFsForWords(words: Seq[Word]) = {
    var counter = 1

    idfForWords =
      idfForWords ++
        words
          .filter(!idfForWords.keySet.contains(_))
          .par
          .map { word =>
            (word, math.log(1.0 + (numberOfDocuments / (documents.count(doc => doc contains word).toDouble + 1.0))))
          }
  }


  def classify(inputText: List[Word]): Seq[Double] = {
    //mabe use Streams for working on already calculated idfs?
    calculateIDFsForWords(inputText)
    val groupedWords = inputText.groupBy(identity).mapValues(_.length)
    val maxUsedWordCount = groupedWords.max._2
    val zipped = wordsPerClass.zip(perClassWordAppearance)

    val classWiseProbabilities = zipped.map {
      case (totalWordsClass, individualWordCountMap) =>
        inputText.map { word =>
          val tfidfForWord = (groupedWords(word).toDouble / maxUsedWordCount.toDouble) * idfForWords(word)
          println(s"TF*Idf for $word = $tfidfForWord")
          (individualWordCountMap.getOrElse(word, 0.0) + 1.0) / (totalWordsClass + vocabularySize) * tfidfForWord
        }
    }

    classWiseProbabilities
      .map(_.product)
      .zip(probabilityPerClass)
      .map {
        case (wordInClassProbability, generalClasProbability) =>
          wordInClassProbability * generalClasProbability
      }
  }
}
