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
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import cats.data.NonEmptyList
import com.google.inject.ImplementedBy

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.ValidationMessage
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateFormat
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateTimeFormat

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaSetConverter
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object JsonValidationService {

  private lazy val formatOverrides = JsonMetaSchema
    .builder(JsonMetaSchema.getV7.getUri, JsonMetaSchema.getV7)
    .addFormat(DateFormat)
    .addFormat(DateTimeFormat)
    .build()

  lazy val factory =
    new JsonSchemaFactory.Builder()
      .defaultMetaSchemaURI(JsonMetaSchema.getV7.getUri)
      .addMetaSchema(formatOverrides)
      .build()
}

@ImplementedBy(classOf[JsonValidationServiceImpl])
trait JsonValidationService {

  def validate(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]]
}

class JsonValidationServiceImpl @Inject() extends JsonValidationService {

  private val mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  val schemaValidators = MessageType.values
    .map(
      msgType => msgType.code -> JsonValidationService.factory.getSchema(getClass.getResourceAsStream(msgType.jsonSchemaPath))
    )
    .toMap

  override def validate(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]] =
    MessageType.values.find(_.code == messageType) match {
      case None =>
        //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
        source.runWith(Sink.ignore)
        Future.successful(Left(NonEmptyList.one(ValidationError.fromUnrecognisedMessageType(messageType))))
      case Some(mType) =>
        val schemaValidator = schemaValidators(mType.code)
        validateJson(source, schemaValidator) match {
          case Success(errors) if errors.isEmpty => Future.successful(Right(()))
          case Success(errors) =>
            val validationErrors = errors.map(
              e => JsonSchemaValidationError(e.getSchemaPath, e.getMessage)
            )

            Future.successful(Left(NonEmptyList.fromList(validationErrors.toList).get))
          case Failure(thr) => Future.failed(thr)
        }
    }

  def validateJson(source: Source[ByteString, _], schemaValidator: JsonSchema)(implicit materializer: Materializer): Try[Set[ValidationMessage]] =
    Try {
      val jsonInput          = source.runWith(StreamConverters.asInputStream(20.seconds))
      val jsonNode: JsonNode = mapper.readTree(jsonInput)
      schemaValidator.validate(jsonNode).asScala.toSet
    }
}
