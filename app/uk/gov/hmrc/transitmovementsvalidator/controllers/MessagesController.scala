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

package uk.gov.hmrc.transitmovementsvalidator.controllers

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.fasterxml.jackson.core.JsonParseException
import play.api.Logging
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.models.errors.BaseError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.InternalServiceError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.UnknownMessageTypeValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.JsonValidationService

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class MessagesController @Inject() (cc: ControllerComponents, xmlValidationService: XmlValidationService, jsonValidationService: JsonValidationService)(implicit
  val materializer: Materializer,
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with StreamingParsers
    with ContentTypeRouting {

  def validate(messageType: String): Action[Source[ByteString, _]] =
    contentTypeRoute {
      case Some(MimeTypes.XML)  => validateMessage(messageType, MimeTypes.XML)
      case Some(MimeTypes.JSON) => validateMessage(messageType, MimeTypes.JSON)
    }

  def validateMessage(messageType: String, contentType: String): Action[Files.TemporaryFile] =
    Action.async(parse.temporaryFile(Long.MaxValue)) {
      implicit request =>
        val stream = FileIO.fromPath(request.body)
        (for {
          validationResponse <-
            if (contentType == MimeTypes.XML) xmlValidationService.validate(messageType, stream)
            else jsonValidationService.validate(messageType, stream)
        } yield validationResponse)
          .map {
            case Left(x) =>
              x.head match {
                case UnknownMessageTypeValidationError(m) => BadRequest(Json.toJson(BaseError.badRequestError(m)))
                case _                                    => Ok(Json.toJson(ValidationResponse(x)))
              }
            case Right(_) => NoContent
          }
          .recover {
            case jsonException: JsonParseException =>
              BadRequest(Json.toJson(BaseError.badRequestError(jsonException.getMessage)))
            case NonFatal(e) =>
              InternalServerError(Json.toJson(InternalServiceError.causedBy(e)))
          }
    }

}
