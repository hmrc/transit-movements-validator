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

package uk.gov.hmrc.transitmovementsvalidator.controllers

import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.*
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.controllers.actions.ValidateAcceptRefiner
import uk.gov.hmrc.transitmovementsvalidator.models.APIVersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.errors.*
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.models.MessageFormat as V2MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.models.MessageType as V2MessageType
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageFormat as V3MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageType as V3MessageType
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.BusinessValidationService as V2BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.JsonValidationService as V2JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.XmlValidationService as V2XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.BusinessValidationService as V3BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.JsonValidationService as V3JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.XmlValidationService as V3XmlValidationService

import javax.inject.Inject
import scala.concurrent.*

class MessagesController @Inject() (
  cc: ControllerComponents,
  v2xmlValidationService: V2XmlValidationService,
  v3xmlValidationService: V3XmlValidationService,
  v2jsonValidationService: V2JsonValidationService,
  v3jsonValidationService: V3JsonValidationService,
  v2businessValidationService: V2BusinessValidationService,
  v3businessValidationService: V3BusinessValidationService,
  validateAcceptRefiner: ValidateAcceptRefiner,
  config: AppConfig
)(implicit
  val materializer: Materializer,
  val temporaryFileCreator: TemporaryFileCreator,
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with StreamingParsers
    with ErrorTranslator {

  private def logError(response: ValidationResponse): ValidationResponse = {
    if (config.errorLogging) {
      logger.warn(Json.toJson(response).toString)
    }
    response
  }

  def validate(messageType: String): Action[Source[ByteString, ?]] = validateAcceptRefiner.async(streamFromMemory) {
    implicit request =>
      (request.headers.get(CONTENT_TYPE), request.versionHeader) match {
        case (Some(MimeTypes.XML), APIVersionHeader.V2_1)  => validateV2XmlMessage(messageType, V2MessageFormat.Xml, request)
        case (Some(MimeTypes.JSON), APIVersionHeader.V2_1) => validateV2JsonMessage(messageType, V2MessageFormat.Json, request)
        case (Some(MimeTypes.XML), APIVersionHeader.V3_0)  => validateV3XmlMessage(messageType, V3MessageFormat.Xml, request)
        case (Some(MimeTypes.JSON), APIVersionHeader.V3_0) => validateV3JsonMessage(messageType, V3MessageFormat.Json, request)
        case (Some(x), _)                                  =>
          request.body.runWith(Sink.ignore)
          Future.successful(UnsupportedMediaType(Json.toJson(PresentationError.unsupportedMediaTypeError(s"Content type $x is not supported."))))
        case _ =>
          request.body.runWith(Sink.ignore)
          Future.successful(UnsupportedMediaType(Json.toJson(PresentationError.unsupportedMediaTypeError(s"Content type must be specified."))))
      }
  }

  private def validateV3JsonMessage(
    messageType: String,
    messageFormat: V3MessageFormat[?],
    request: Request[Source[ByteString, ?]]
  ) =
    (for {
      messageTypeObj <- findV3MessageType(messageType)

      // We create a Flow that we can attach to request.body, which allows us to perform the schema validation and
      // business validation at the same time. We get the result from the business rule validation from the
      // deferredBusinessRulesValidation EitherT[Future, ValidationError, Unit], which completes when we pass
      // the source with the flow attached to the schema validation service and it runs.
      (deferredBusinessRulesValidation, businessRulesFlow) = v3businessValidationService.businessValidationFlow(messageTypeObj, messageFormat)
      _ <- v3jsonValidationService.validate(messageTypeObj, request.body.via(businessRulesFlow)).asPresentation
      _ <- deferredBusinessRulesValidation.asPresentation
    } yield NoContent)
      .valueOr {
        case SchemaValidationPresentationError(errors) =>
          // We have special cased this as this isn't considered an "error" so much.
          logError(ValidationResponse(errors))
          Ok(Json.toJson(ValidationResponse(errors)))
        case presentationError =>
          Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))
      }

  private def validateV2JsonMessage(
    messageType: String,
    messageFormat: V2MessageFormat[?],
    request: Request[Source[ByteString, ?]]
  ): Future[Result] =
    (for {
      messageTypeObj <- findV2MessageType(messageType)

      // We create a Flow that we can attach to request.body, which allows us to perform the schema validation and
      // business validation at the same time. We get the result from the business rule validation from the
      // deferredBusinessRulesValidation EitherT[Future, ValidationError, Unit], which completes when we pass
      // the source with the flow attached to the schema validation service and it runs.
      (deferredBusinessRulesValidation, businessRulesFlow) = v2businessValidationService.businessValidationFlow(messageTypeObj, messageFormat)
      _ <- v2jsonValidationService.validate(messageTypeObj, request.body.via(businessRulesFlow)).asPresentation
      _ <- deferredBusinessRulesValidation.asPresentation
    } yield NoContent)
      .valueOr {
        case SchemaValidationPresentationError(errors) =>
          // We have special cased this as this isn't considered an "error" so much.
          logError(ValidationResponse(errors))
          Ok(Json.toJson(ValidationResponse(errors)))
        case presentationError =>
          Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))
      }

  private def validateV2XmlMessage(
    messageType: String,
    messageFormat: V2MessageFormat[?],
    request: Request[Source[ByteString, ?]]
  ): Future[Result] =
    (for {
      messageTypeObj <- findV2MessageType(messageType)

      // We create a Flow that we can attach to request.body, which allows us to perform the schema validation and
      // business validation at the same time. We get the result from the business rule validation from the
      // deferredBusinessRulesValidation EitherT[Future, ValidationError, Unit], which completes when we pass
      // the source with the flow attached to the schema validation service and it runs.
      (deferredBusinessRulesValidation, businessRulesFlow) = v2businessValidationService.businessValidationFlow(messageTypeObj, messageFormat)
      _ <- v2xmlValidationService.validate(messageTypeObj, request.body.via(businessRulesFlow)).asPresentation
      _ <- deferredBusinessRulesValidation.asPresentation
    } yield NoContent)
      .valueOr {
        case SchemaValidationPresentationError(errors) =>
          logError(ValidationResponse(errors))
          // We have special cased this as this isn't considered an "error" so much.
          Ok(Json.toJson(ValidationResponse(errors)))
        case presentationError =>
          Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))
      }

  private def validateV3XmlMessage(
    messageType: String,
    messageFormat: V3MessageFormat[?],
    request: Request[Source[ByteString, ?]]
  ): Future[Result] =
    (for {
      messageTypeObj <- findV3MessageType(messageType)

      // We create a Flow that we can attach to request.body, which allows us to perform the schema validation and
      // business validation at the same time. We get the result from the business rule validation from the
      // deferredBusinessRulesValidation EitherT[Future, ValidationError, Unit], which completes when we pass
      // the source with the flow attached to the schema validation service and it runs.
      (deferredBusinessRulesValidation, businessRulesFlow) = v3businessValidationService.businessValidationFlow(messageTypeObj, messageFormat)
      _ <- v3xmlValidationService.validate(messageTypeObj, request.body.via(businessRulesFlow)).asPresentation
      _ <- deferredBusinessRulesValidation.asPresentation
    } yield NoContent)
      .valueOr {
        case SchemaValidationPresentationError(errors) =>
          // We have special cased this as this isn't considered an "error" so much.
          logError(ValidationResponse(errors))
          Ok(Json.toJson(ValidationResponse(errors)))
        case presentationError =>
          Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))
      }

  private def findV2MessageType(messageType: String): EitherT[Future, PresentationError, V2MessageType] =
    EitherT
      .fromEither(
        V2MessageType.find(messageType, config.validateRequestTypesOnly).toRight[ValidationError](ValidationError.UnknownMessageType(messageType))
      )
      .asPresentation

  private def findV3MessageType(messageType: String): EitherT[Future, PresentationError, V3MessageType] =
    EitherT
      .fromEither(
        V3MessageType.find(messageType, config.validateRequestTypesOnly).toRight[ValidationError](ValidationError.UnknownMessageType(messageType))
      )
      .asPresentation

}
