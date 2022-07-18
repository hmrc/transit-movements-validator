/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.eclipsesource.schema.SchemaType
import com.eclipsesource.schema.SchemaValidator
import com.eclipsesource.schema.drafts.Version7
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.nio.file.Paths
import scala.concurrent.Future

trait JsonValidation {

  val toStringFlow = Flow[ByteString].map(_.utf8String)

  val jsonParseFlow = Flow[String].map(Json.parse)

  val validator = SchemaValidator(Some(Version7))

  def parseJsonSchema(path: String)(implicit materializer: Materializer): Future[JsValue] =
    FileIO.fromPath(Paths.get(path)).via(toStringFlow).via(jsonParseFlow).runWith(Sink.head[JsValue])

  def validate(source: Source[ByteString, _], schemaType: SchemaType)(implicit materializer: Materializer): Future[JsResult[JsValue]] =
    source.via(toStringFlow).via(jsonParseFlow).map(validator.validate(schemaType, _)).runWith(Sink.head[JsResult[JsValue]])
}
