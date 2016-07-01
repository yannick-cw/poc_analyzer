package utils

import elasicsearch_loader.Queries.CleanedDoc

/**
  * Created by Yannick on 20/06/16.
  */
trait Model {
  val name: String
  def classify(inputText: CleanedDoc, useTfIdf: Boolean = false): Seq[Double]
}
