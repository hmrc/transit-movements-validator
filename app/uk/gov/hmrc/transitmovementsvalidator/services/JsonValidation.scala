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
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import uk.gov.hmrc.transitmovementsvalidator.models.MessageTypeJson

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

trait JsonValidation {

  private val mapper  = new ObjectMapper
  private val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)

  val schemaValidators = MessageTypeJson.values
    .map(
      msgType => msgType.code -> factory.getSchema(getClass.getResourceAsStream(msgType.schemaPath))
    )
    .toMap

  def validateJson(source: Source[ByteString, _], schemaValidator: JsonSchema)(implicit materializer: Materializer): Set[ValidationMessage] = {
    val jsonInput          = source.runWith(StreamConverters.asInputStream(20.seconds))
    val jsonNode: JsonNode = mapper.readTree(jsonInput)
    schemaValidator.validate(jsonNode).asScala.toSet
  }
}
