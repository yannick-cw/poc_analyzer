package tf_idf

import elasicsearch_loader.Queries.{CleanedDoc, Hit}
import naive_bayes.BayesModel.{Class, Doc, Word}


case class DocumentCorpus(
                           var wordsInDocuments: List[Option[Doc]] = List.empty,
                           var numberOfDocuments: Int = 0,
                           idfsForWords: Map[Word, Double] = Map.empty
                         )

case class TfIdfResult(resultTfIdfs: Map[Word, Double])

object TfIdfHelper {

    val documentCorpus = DocumentCorpus()


    def updateData(words: List[Hit]) = {

        documentCorpus.wordsInDocuments = words
          .map(_._source)
          .map(_.cleanedText.split(" ").toList)
          .map(Some(_))

        documentCorpus.numberOfDocuments = words.size

    }


    def calculate(userWords: Seq[Word]): TfIdfResult = {

        def calculateTFs = {

            val groupedSameWords = userWords.groupBy(identity)
            val maxOcurenceOfWord = groupedSameWords.values.map(_.size).max

            groupedSameWords.map { word => {
                (word._1, word._2.size.toDouble / maxOcurenceOfWord.toDouble)
            }
            }
        }


        def updateIDFs() = {

            val allWords = documentCorpus.wordsInDocuments.flatten.distinct

            userWords.zipWithIndex.foreach { case (word, count) => {
                documentCorpus.idfsForWords.updated(
                    word,
                    math.log(1.0 + (documentCorpus.numberOfDocuments / (documentCorpus.wordsInDocuments.count(_.contains(word)) + 1)))
                )
            }
            }

        }

        val tfs = calculateTFs

        def calculateTfIdfForClass = {
            tfs.map { case (word, tfForWord) => {
                word -> tfForWord * documentCorpus.idfsForWords.getOrElse(word, math.log(1 + documentCorpus.numberOfDocuments))
            }
            }
        }

        updateIDFs()
        TfIdfResult(calculateTfIdfForClass)

    }

}