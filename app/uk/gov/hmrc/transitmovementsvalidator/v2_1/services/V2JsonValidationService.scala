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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.services

import cats.data.EitherT
import cats.data.NonEmptyList
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.ImplementedBy
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.ValidationMessage
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.StreamConverters
import org.apache.pekko.util.ByteString
import play.api.Logging
import uk.gov.hmrc.transitmovementsvalidator.models.APIVersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.FailedToParse
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.JsonFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.Unexpected
import uk.gov.hmrc.transitmovementsvalidator.services.ValidationService
import uk.gov.hmrc.transitmovementsvalidator.utils.jsonformats.DateFormat
import uk.gov.hmrc.transitmovementsvalidator.utils.jsonformats.DateTimeFormat

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.Using

object V2JsonValidationService {

  private lazy val formatOverrides = JsonMetaSchema
    .builder(JsonMetaSchema.getV7.getUri, JsonMetaSchema.getV7)
    .addFormat(DateFormat)
    .addFormat(DateTimeFormat)
    .build()

  lazy val factory: JsonSchemaFactory =
    new JsonSchemaFactory.Builder()
      .defaultMetaSchemaURI(JsonMetaSchema.getV7.getUri)
      .addMetaSchema(formatOverrides)
      .build()
}

@ImplementedBy(classOf[V2JsonValidationServiceImpl])
trait V2JsonValidationService extends ValidationService

class V2JsonValidationServiceImpl @Inject() extends V2JsonValidationService with Logging {

  private val mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

  private val schemaValidators = MessageType
    .values(APIVersionHeader.V2_1)
    .map(
      msgType => msgType.code -> V2JsonValidationService.factory.getSchema(getClass.getResourceAsStream(msgType.jsonSchemaPath))
    )
    .toMap

  override def validate(messageType: MessageType, source: Source[ByteString, ?])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      val schemaValidator = schemaValidators(messageType.code)
      validateJson(source, schemaValidator) match {
        case Success(errors) if errors.isEmpty => Future.successful(Right(()))
        case Success(errors) =>
          val validationErrors = errors.map(
            e => JsonSchemaValidationError(e.getSchemaPath, e.getMessage)
          )

          Future.successful(Left(JsonFailedValidation(NonEmptyList.fromListUnsafe(validationErrors.toList))))
        case Failure(thr: JsonParseException) => Future.successful(Left(FailedToParse(stripSource(thr.getMessage))))
        case Failure(thr) =>
          logger
            .error(s"Validate Json Internal server error occurred : $thr", thr); Future.successful(Left(Unexpected(Some(thr))))
      }
    }

  private def validateJson(source: Source[ByteString, ?], schemaValidator: JsonSchema)(implicit materializer: Materializer): Try[Set[ValidationMessage]] =
    Using(source.runWith(StreamConverters.asInputStream(20.seconds))) {
      jsonInput =>
        val jsonNode: JsonNode = mapper.readTree(jsonInput)
        schemaValidator.validate(jsonNode).asScala.toSet
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
    message.replace("Source: (org.apache.pekko.stream.impl.io.InputStreamAdapter); ", "")

}
