package tf_idf

import elasicsearch_loader.Queries.CleanedDoc
import naive_bayes.BayesModel
import org.scalatest.{MustMatchers, WordSpecLike}

/**
  * Created by Yannick on 23.05.16.
  */
class TfIdfSpec extends WordSpecLike with MustMatchers {

  "A BayesModel" must {
    "classify right for simple input" in {
      val classA: List[List[String]] = List(List("Chinese", "Beijing", "Chinese"), List("Chinese", "Chinese", "Shanghai"), List("Chinese", "Macao"))

      val classB: List[List[String]] = List(List("Tokyo", "Japan", "Chinese"))

      val testInput = CleanedDoc("", 0, "", "Chinese Chinese Chinese Tokyo Japan")

      val model = BayesModel(classA, classB)
      model.classify(testInput) must be(Seq(3.0121377997263036E-4,1.3548070246744226E-4))
    }
  }

}
