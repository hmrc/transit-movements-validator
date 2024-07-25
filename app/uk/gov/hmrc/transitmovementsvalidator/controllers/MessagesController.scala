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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import play.api.Logging
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors._
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services._
import uk.gov.hmrc.transitmovementsvalidator.stream.StreamingParsers

import javax.inject.Inject
import scala.concurrent._

class MessagesController @Inject() (
  cc: ControllerComponents,
  xmlValidationService: XmlValidationService,
  jsonValidationService: JsonValidationService,
  businessValidationService: BusinessValidationService,
  config: AppConfig
)(implicit
  val materializer: Materializer,
  val temporaryFileCreator: TemporaryFileCreator,
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with StreamingParsers
    with ContentTypeRouting
    with ErrorTranslator {

  def validate(messageType: String): Action[Source[ByteString, _]] =
    contentTypeRoute {
      case Some(MimeTypes.XML)  => validateMessage(messageType, xmlValidationService, MessageFormat.Xml)
      case Some(MimeTypes.JSON) => validateMessage(messageType, jsonValidationService, MessageFormat.Json)
    }

  private def validateMessage(messageType: String, validationService: ValidationService, messageFormat: MessageFormat[_]): Action[Source[ByteString, _]] =
    Action.async(streamFromMemory) {
      implicit request =>
        (for {
          messageTypeObj <- findMessageType(messageType)

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

    }

  private def findMessageType(messageType: String): EitherT[Future, PresentationError, MessageType] =
    EitherT
      .fromEither(MessageType.find(messageType, config.validateRequestTypesOnly).toRight[ValidationError](ValidationError.UnknownMessageType(messageType)))
      .asPresentation

}
