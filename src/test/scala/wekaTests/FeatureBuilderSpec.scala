package wekaTests

import org.scalatest.{FlatSpec, FunSpec, Matchers}

/**
  * Created by Yannick on 14/06/16.
  */
class FeatureBuilderSpec extends FunSpec with Matchers with FeatureBuilder {

  describe("FeatureBuilder") {
    it("should find all spelling errors in Doc") {
      val text = "Heree, arre some errorrs!"
      mistakesPerWord(text) should be (3.0/4.0)
    }

    it("should give back the average sentence length") {
      val text = "This is four words. This is even more, words! How many words are in this?"
      normalizedSentenceLength(text) should be (5.0)
    }

    it("should find the average uppercase letters per word") {
      val text = "WHAAT THE FUCK"
      val t2 = "Just normal text"

      normalizedUppercaseLetters(text) should be(4.0)
      normalizedUppercaseLetters(t2) should be(1.0/3.0)
    }

    it("should find the average word length") {
      val text = "Hei, how, low, are the wor!!!"

      normalizedWordLength(text) should be(3.0)
    }

    it("should give back normalized distinct words") {
      val text = "Hei, how, low, are the wor!!!"
      val t2 = "eins eins zwei zwei"
      normalizedDistinctWords(text) should be(1.0)
      normalizedDistinctWords(t2) should be(0.5)
    }
  }

}
