package elasicsearch_loader

/**
  * Created by Yannick on 23.05.16.
  */
object Queries {
  case class CleanedDoc(src: String, ups: Int, rawText: String, cleanedText: String)
  case class Hit(_index: String, _type: String, _id: String, _score: Double, _source: CleanedDoc)
  case class Hits(total: Int, max_score: Int, hits: List[Hit])
  case class Shards(total: Int, successful: Int, failed: Int)
  case class ScrollResponse(_scroll_id: String, took: Int, timed_out: Boolean, _shards: Shards, hits: Hits)
}
