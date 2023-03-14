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
import play.api.libs.functional.Functor
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.transitmovementsvalidator.controllers.MessagesController.ResponseCreator
import uk.gov.hmrc.transitmovementsvalidator.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationPresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.ObjectStoreService
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

class MessagesController @Inject() (
  cc: ControllerComponents,
  xmlValidationService: XmlValidationService,
  jsonValidationService: JsonValidationService,
  objectStoreService: ObjectStoreService
)(implicit
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
      case None                 => validateObjectStoreMessage(messageType, xmlValidationService)
    }

  private val failCase: PresentationError => Result = presentationError =>
    Status(presentationError.code.statusCode)(Json.toJson(presentationError)(PresentationError.presentationErrorWrites))

  def validateObjectStoreMessage(messageType: String, validationService: ValidationService): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromRequest(request)
      (for {
        uri      <- getObjectStoreUri
        contents <- objectStoreService.getContents(uri)(executionContext, hc).asPresentation
        result   <- validationService.validate(messageType, contents).asPresentation.toValidationResponse
      } yield result)
        .fold[Result](
          failCase,
          {
            case Some(r) => Ok(Json.toJson(r))
            case None    => NoContent
          }
        )
  }

  def validateMessage(messageType: String, validationService: ValidationService): Action[Source[ByteString, _]] =
    Action.async(streamFromMemory) {
      implicit request =>
        validationService
          .validate(messageType, request.body)
          .asPresentation
          .toValidationResponse
          .fold(
            failCase,
            {
              case Some(r) => Ok(Json.toJson(r))
              case None    => NoContent
            }
          )
    }

  private def getObjectStoreUri(implicit request: RequestHeader) = EitherT {
    Future.successful(
      request.headers
        .get("X-Object-Store-Uri")
        .toRight(PresentationError.unsupportedMediaTypeError("Content Type or X-Object-Store-Uri must be specified."))
    )
  }

}
