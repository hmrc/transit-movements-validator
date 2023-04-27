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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.ImplementedBy
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import uk.gov.hmrc.transitmovementsvalidator.models.CustomValidationMessage
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError._
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateFormat
import uk.gov.hmrc.transitmovementsvalidator.services.jsonformats.DateTimeFormat

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
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

  def findNodes(jsonNode: JsonNode, targetNodes: List[String]): List[JsonNode] = {
    val foundNodes = new ListBuffer[JsonNode]()

    def traverse(node: JsonNode): Unit =
      if (node.isObject) {
        node.fields().forEachRemaining {
          field =>
            if (targetNodes.contains(field.getKey)) {
              foundNodes += field.getValue
            } else {
              traverse(field.getValue)
            }
        }
      } else if (node.isArray) {
        node.elements().forEachRemaining {
          arrayElement =>
            traverse(arrayElement)
        }
      }

    traverse(jsonNode)
    foundNodes.toList
  }

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
            case Success(errors) if !errors.head.isBusinessValidation =>
              val validationErrors = errors.map(
                e => JsonSchemaValidationError(e.schemaPath.get, e.message)
              )
              Future.successful(Left(JsonFailedValidation(NonEmptyList.fromListUnsafe(validationErrors.toList))))

            case Success(errors) if errors.head.isBusinessValidation =>
              Future.successful(Left(BusinessValidationError(errors.head.message)))

            case Failure(thr: JsonParseException) => Future.successful(Left(FailedToParse(stripSource(thr.getMessage))))
            case Failure(thr)                     => Future.successful(Left(Unexpected(Some(thr))))
            case _                                => Future.successful(Left(Unexpected(None))) // add this line to handle the case when validateJson returns a Failure
          }
      }
    }

  def validateJson(source: Source[ByteString, _], schemaValidator: JsonSchema)(implicit materializer: Materializer): Try[Set[CustomValidationMessage]] =
    Try {
      Using(source.runWith(StreamConverters.asInputStream(20.seconds))) {
        jsonInput =>
          val jsonNode: JsonNode      = mapper.readTree(jsonInput)
          val rootNode                = jsonNode.fields().next().getKey
          val messageType             = jsonNode.path(rootNode).path("messageType").textValue()
          val messageTypeFromRootNode = rootNode.split(":")(1)

          if (!messageTypeFromRootNode.equalsIgnoreCase(messageType)) {
            Set(CustomValidationMessage(None, "Root node doesn't match with the messageType", isBusinessValidation = true))
          } else {
            val schemaValidationErrors: Set[CustomValidationMessage] =
              schemaValidator
                .validate(jsonNode)
                .asScala
                .map(
                  vm => CustomValidationMessage(Some(vm.getSchemaPath), vm.getMessage, isBusinessValidation = false)
                )
                .toSet

            val customsOfficeNodes = List("CustomsOfficeOfDeparture", "CustomsOfficeOfDestinationActual")

            val targetNodes = findNodes(jsonNode, customsOfficeNodes)

            val referenceNumberErrors = targetNodes.flatMap {
              customsOfficeNode =>
                val referenceNumberNode = customsOfficeNode.path("referenceNumber")
                if (referenceNumberNode.isMissingNode) {
                  None
                } else {
                  val referenceNumber = referenceNumberNode.asText()
                  if (!referenceNumber.startsWith("GB") && !referenceNumber.startsWith("XI")) {
                    Some(CustomValidationMessage(None, s"Invalid reference number must start with 'GB' or 'XI'.", isBusinessValidation = true))
                  } else {
                    None
                  }
                }
            }.toSet

            schemaValidationErrors ++ referenceNumberErrors
          }
      }.get
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
