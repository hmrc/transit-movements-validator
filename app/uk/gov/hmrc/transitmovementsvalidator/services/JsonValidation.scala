package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.nio.file.Paths
import scala.concurrent.Future

trait JsonValidation {

  val toStringFlow = Flow[ByteString].map(_.utf8String)

  val jsonParseFlow = Flow[String].map(Json.parse)

  def parseJsonSchema(path: String)(implicit materializer: Materializer): Future[JsValue] =
    FileIO.fromPath(Paths.get(path)).via(toStringFlow).via(jsonParseFlow).runWith(Sink.head[JsValue])

}
