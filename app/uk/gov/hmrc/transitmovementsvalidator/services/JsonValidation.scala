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
import com.eclipsesource.schema.drafts.Version7.schemaTypeReads
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import com.eclipsesource.schema.SchemaType
import com.eclipsesource.schema.SchemaValidator
import com.eclipsesource.schema.drafts.Version7
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait JsonValidation {

  val validator = SchemaValidator(Some(Version7))

  def getSchemaType(filepath: String): SchemaType = {
    val stream     = getClass.getResourceAsStream(filepath)
    val raw        = scala.io.Source.fromInputStream(stream).getLines.mkString
    val jsonSchema = Json.parse(raw)
    Json.fromJson[SchemaType](jsonSchema).get
  }

  def validateJson(source: Source[ByteString, _], schemaType: SchemaType)(implicit materializer: Materializer): Future[JsResult[JsValue]] = {
    val stream = source.runWith(StreamConverters.asInputStream(20.seconds))
    val raw    = scala.io.Source.fromInputStream(stream).getLines.mkString
    val json   = Json.parse(raw)
    Future.successful(validator.validate(schemaType, json))
  }
}
