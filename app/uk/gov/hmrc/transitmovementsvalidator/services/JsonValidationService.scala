/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.data.EitherT
import cats.data.NonEmptyList
import com.fasterxml.jackson.core.JsonParseException
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
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.FailedToParse
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.JsonFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.Unexpected
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.UnknownMessageType
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateFormat
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateTimeFormat

import javax.inject.Inject
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.util.Using

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
trait JsonValidationService extends ValidationService

class JsonValidationServiceImpl @Inject() extends JsonValidationService {

  private val mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

  val schemaValidators = MessageType.values
    .map(
      msgType => msgType.code -> JsonValidationService.factory.getSchema(getClass.getResourceAsStream(msgType.jsonSchemaPath))
    )
    .toMap

  override def validate(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      MessageType.values.find(_.code == messageType) match {
        case None =>
          //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
          source.runWith(Sink.ignore)
          Future.successful(Left(UnknownMessageType(messageType)))
        case Some(mType) =>
          val schemaValidator = schemaValidators(mType.code)
          validateJson(source, schemaValidator) match {
            case Success(errors) if errors.isEmpty => Future.successful(Right(()))
            case Success(errors) =>
              val validationErrors = errors.map(
                e => JsonSchemaValidationError(e.getSchemaPath, e.getMessage)
              )

              Future.successful(Left(JsonFailedValidation(NonEmptyList.fromListUnsafe(validationErrors.toList))))
            case Failure(thr: JsonParseException) => Future.successful(Left(FailedToParse(stripSource(thr.getMessage))))
            case Failure(thr)                     => Future.successful(Left(Unexpected(Some(thr))))
          }
      }
    }

  override def businessRuleValidation(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      MessageType.values.find(_.code == messageType) match {
        case Some(_) =>
          validateJson(source) match {
            case Success(errors) if errors.isEmpty => Future.successful(Right(()))
            case Success(errors)                   => Future.successful(Left(errors.head))
            case Failure(thr)                      => Future.successful(Left(Unexpected(Some(thr))))
          }
        case None =>
          Future.successful(Left(UnknownMessageType(messageType)))
      }
    }

  def validateJson(source: Source[ByteString, _], schemaValidator: JsonSchema)(implicit materializer: Materializer): Try[Set[ValidationMessage]] =
    Using(source.runWith(StreamConverters.asInputStream(20.seconds))) {
      jsonInput =>
        val jsonNode: JsonNode = mapper.readTree(jsonInput)
        schemaValidator.validate(jsonNode).asScala.toSet
    }

  def validateJson(source: Source[ByteString, _])(implicit materializer: Materializer): Try[Set[BusinessValidationError]] =
    Using(source.runWith(StreamConverters.asInputStream(20.seconds))) {
      jsonInput =>
        val jsonNode: JsonNode      = mapper.readTree(jsonInput)
        val rootNode                = jsonNode.fields().next().getKey
        val messageType             = jsonNode.path(rootNode).path("messageType").textValue()
        val messageTypeFromRootNode = rootNode.split(":")(1)
        if (!messageTypeFromRootNode.equalsIgnoreCase(messageType)) {
          Set(BusinessValidationError("Root node doesn't match with the messageType"))
        } else {
          Set()
        }
    }

  // Unfortunately, the JsonParseException contains an implementation detail that's just going to confuse
  // software developers. So, we'll replace the string containing the "source" with nothing, so that the
  // message reads "at [line: x, column: y]".
  //
  // I looked for a better way to do this by trying to alter how the ObjectMapper worked, but the only solution
  // that I could see was to use reflection to get the message without the location and construct that manually,
  // but that's more trouble than it's worth.
  //
  // A test will break if this stops working due to upgrades.
  def stripSource(message: String): String =
    message.replace("Source: (akka.stream.impl.io.InputStreamAdapter); ", "")
}
