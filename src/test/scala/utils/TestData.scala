package utils

import elasicsearch_loader.Queries.CleanedDoc

import scala.io.Source


/**
  * Created by yannick on 10.05.16.
  */
object TestData {
  val initIdResponse = Source.fromFile(getClass.getResource("/scroll_init_response.json").getPath).mkString
  val scrollResponse = Source.fromFile(getClass.getResource("/scroll_response.json").getPath).mkString

  val initId = "c2Nhbjs1Ozk2OnpETUJ4UlplU0JleUN1QkI4WElPR0E7OTc6ekRNQnhSWmVTQmV5Q3VCQjhYSU9HQTsxMDA6ekRNQnhSWmVTQmV5Q3VCQjhYSU9HQTs5ODp6RE1CeFJaZVNCZXlDdUJCOFhJT0dBOzk5OnpETUJ4UlplU0JleUN1QkI4WElPR0E7MTt0b3RhbF9oaXRzOjE4MDk2NDs="

  val hits = Set(CleanedDoc("Republican", 1,  "I would upvote this a thousand times if I could",  "i upvot thousand time i")
    , CleanedDoc("Republican", 2, "Here is what history will need to forget:\\n\\n* The Patriot Act\\n\\n* No Child Left Behind\\n\\n* Iraq\\n\\n* Hurricane Katrina", "histori need forgetnn patriot actnn child left behindnn iraqnn hurrican katrina"))
}