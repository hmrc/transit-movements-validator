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

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.controllers.MessagesController.ResponseCreator
import uk.gov.hmrc.transitmovementsvalidator.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationPresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.ValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.XmlValidationService

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object MessagesController {

  implicit class ResponseCreator(val value: EitherT[Future, PresentationError, Unit]) extends AnyVal {

    def toValidationResponse(implicit ec: ExecutionContext): EitherT[Future, PresentationError, Option[ValidationResponse]] =
      value.transform {
        case Left(SchemaValidationPresentationError(errors)) => Right(Some(ValidationResponse(errors)))
        case Left(x)                                         => Left(x)
        case Right(_)                                        => Right(None)
      }
  }
}

class MessagesController @Inject() (cc: ControllerComponents, xmlValidationService: XmlValidationService, jsonValidationService: JsonValidationService)(implicit
  val materializer: Materializer,
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with StreamingParsers
    with ContentTypeRouting
    with ErrorTranslator {

  def validate(messageType: String): Action[Source[ByteString, _]] =
    contentTypeRoute {
      case Some(MimeTypes.XML)  => validateMessage(messageType, xmlValidationService)
      case Some(MimeTypes.JSON) => validateMessage(messageType, jsonValidationService)
    }

  def validateMessage(messageType: String, validationService: ValidationService): Action[Source[ByteString, _]] =
    Action.async(streamFromMemory) {
      implicit request =>
        validationService
          .validate(messageType, request.body)
          .asPresentation
          .toValidationResponse
          .fold(
            presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites)),
            {
              case Some(r) => Ok(Json.toJson(r))
              case None    => NoContent
            }
          )

    }

}
