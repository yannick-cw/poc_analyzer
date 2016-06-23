package tf_idf

import akka.actor.{Actor, ActorRef, Props}
import elasicsearch_loader.LoadActor.FinishedImport
import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.BayesModel.Word

case class ClassInformation(cleanedDoc: List[CleanedDoc], numberOfDocuments: Int, var idfsForWords: Map[Word, Double] = Map.empty)
case class TfIdfRequest(requestedWords: Seq[Word])
case class TfIdfResult(resultTfIdfs: Seq[Map[Word, Double]])

object TfIdfHelperActor {
    def props(master: ActorRef) = Props(new TfIdfHelperActor(master))
}

class TfIdfHelperActor(master: ActorRef) extends Actor {

    def receive: Receive = waitingForClasses


    def waitingForClasses: Receive = {
        case FinishedImport(_, _, hits) => {
            val (democrats, republican) = hits.partition(_._index == "dem")
            val republicanDocuments = republican.map(_._source)
            val democratsDocuments: List[CleanedDoc] = democrats.map(_._source)
            context become readyForClassification(ClassInformation(republicanDocuments, republicanDocuments.size), ClassInformation(democratsDocuments, democratsDocuments.size))
        }
    }


    def readyForClassification(republicanDocuments: ClassInformation, democratsDocuments: ClassInformation): Receive = {
        case calculateWords@TfIdfRequest(requestedWords) => {
            val result = calculateIDF(requestedWords, republicanDocuments, democratsDocuments)

            republicanDocuments.idfsForWords = result._2(0)
            democratsDocuments.idfsForWords = result._2(1)

            master ! TfIdfResult(result._1)
            context become readyForClassification(republicanDocuments, democratsDocuments)
        }
    }


    def calculateIDF(wordsToClassify: Seq[Word], classDocuments: ClassInformation*) = {
        val getWords: (CleanedDoc => List[Word]) = doc => doc.cleanedText.split(" ").toList

        val updatedIdfs = classDocuments.map { classData => {
            classData.idfsForWords ++ wordsToClassify
              .filter(classData.idfsForWords.keySet.contains(_))
              .map { word => {
                  (word, math.log(1.0 + (classData.numberOfDocuments / classData.cleanedDoc.count(doc => {
                      getWords(doc).contains(word)
                  }))))
              }}.toMap
        }}

        val tfForWords = calculateTF(wordsToClassify)

        val tfIdfsForUserInput = updatedIdfs.map { idfsForClass => {
            tfForWords.map { wordTf => {
                (wordTf._1, wordTf._2 * idfsForClass(wordTf._1))
            }}
        }}

        (tfIdfsForUserInput, updatedIdfs)

    }


    def calculateTF(wordsToClassify: Seq[Word]) = {
        val groupedSameWords = wordsToClassify.groupBy(identity)
        val maxOcurenceOfWord = groupedSameWords.max._2.size

        groupedSameWords.map { word => {
            (word._1, word._2.size.toDouble / maxOcurenceOfWord.toDouble)
        }}
    }

}
