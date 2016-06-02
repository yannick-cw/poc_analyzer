package util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import elasicsearch_loader.Queries._
import rest_connection.{ClassifyRequest, ClassifyResult, CleanedText, RawText}
import spray.json.DefaultJsonProtocol

trait Protocols extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val classifyRequestForm = jsonFormat2(ClassifyRequest.apply)
  implicit val cleanedDocForm = jsonFormat4(CleanedDoc.apply)
  implicit val hitForm = jsonFormat5(Hit.apply)
  implicit val hitsForm = jsonFormat3(Hits.apply)
  implicit val shardsForm = jsonFormat3(Shards.apply)
  implicit val scrollResponseForm = jsonFormat5(ScrollResponse.apply)
  implicit val rawTextForm = jsonFormat1(RawText.apply)
  implicit val cleanedTextForm = jsonFormat1(CleanedText.apply)
  implicit val classifyResultForm = jsonFormat3(ClassifyResult.apply)
}
