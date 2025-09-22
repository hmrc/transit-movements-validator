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
import uk.gov.hmrc.transitmovementsvalidator.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.*
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.ValidationService
import uk.gov.hmrc.transitmovementsvalidator.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.*
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.V3BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.V3JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.V3XmlValidationService

import javax.inject.Inject
import scala.concurrent.*

class MessagesController @Inject() (
  cc: ControllerComponents,
  v2XmlValidationService: V2XmlValidationService,
  v2JsonValidationService: V2JsonValidationService,
  v2BusinessValidationService: V2BusinessValidationService,
  v3XmlValidationService: V3XmlValidationService,
  v3JsonValidationService: V3JsonValidationService,
  v3BusinessValidationService: V3BusinessValidationService,
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

  def validate(messageType: String): Action[Source[ByteString, ?]] = validateAcceptRefiner.async(streamFromMemory) {
    implicit request =>
      val services: (ValidationService, ValidationService, BusinessValidationService) = request.versionHeader match {
        case APIVersionHeader.V2_1 => (v2XmlValidationService, v2JsonValidationService, v2BusinessValidationService)
        case APIVersionHeader.V3_0 => (v3XmlValidationService, v3JsonValidationService, v3BusinessValidationService)
      }
      request.headers.get(CONTENT_TYPE) match {
        case Some(MimeTypes.XML) =>
          validateMessage(messageType, services._1, MessageFormat.Xml, services._3, request, request.versionHeader)
        case Some(MimeTypes.JSON) =>
          validateMessage(messageType, services._2, MessageFormat.Json, services._3, request, request.versionHeader)
        case Some(x) =>
          request.body.runWith(Sink.ignore)
          Future.successful(UnsupportedMediaType(Json.toJson(PresentationError.unsupportedMediaTypeError(s"Content type $x is not supported."))))
        case _ =>
          request.body.runWith(Sink.ignore)
          Future.successful(UnsupportedMediaType(Json.toJson(PresentationError.unsupportedMediaTypeError(s"Content type must be specified."))))
      }
  }

  private def validateMessage(
    messageType: String,
    validationService: ValidationService,
    messageFormat: MessageFormat[?],
    businessValidationService: BusinessValidationService,
    request: Request[Source[ByteString, ?]],
    versionHeader: APIVersionHeader
  ): Future[Result] =
    (for {
      messageTypeObj <- findMessageType(messageType, versionHeader)

      // We create a Flow that we can attach to request.body, which allows us to perform the schema validation and
      // business validation at the same time. We get the result from the business rule validation from the
      // deferredBusinessRulesValidation EitherT[Future, ValidationError, Unit], which completes when we pass
      // the source with the flow attached to the schema validation service and it runs.
      (deferredBusinessRulesValidation, businessRulesFlow) = businessValidationService.businessValidationFlow(messageTypeObj, messageFormat)
      _ <- validationService.validate(messageTypeObj, request.body.via(businessRulesFlow)).asPresentation
      _ <- deferredBusinessRulesValidation.asPresentation
    } yield NoContent)
      .valueOr {
        case SchemaValidationPresentationError(errors) =>
          // We have special cased this as this isn't considered an "error" so much.
          Ok(Json.toJson(ValidationResponse(errors)))
        case presentationError =>
          Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))
      }

  private def findMessageType(messageType: String, versionHeader: APIVersionHeader): EitherT[Future, PresentationError, MessageType] =
    EitherT
      .fromEither(
        MessageType.find(messageType, config.validateRequestTypesOnly, versionHeader).toRight[ValidationError](ValidationError.UnknownMessageType(messageType))
      )
      .asPresentation

}
