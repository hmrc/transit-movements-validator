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
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.models.errors.TransitMovementError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.UnsupportedMediaTypeError
import uk.gov.hmrc.transitmovementsvalidator.models.formats.HttpFormats
import uk.gov.hmrc.transitmovementsvalidator.models.response.FailedValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.models.response.SuccessfulValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.ValidationService

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MessagesController @Inject() (cc: ControllerComponents, validationService: ValidationService)(implicit
  val materializer: Materializer,
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with StreamingParsers
    with HttpFormats {

  def validate(messageType: String): Action[Source[ByteString, _]] = Action.async(streamFromMemory) {
    implicit request =>
      request.headers.get(CONTENT_TYPE) match {
        case Some(
              MimeTypes.XML
            ) => // As an internal service, we can control just sending this mime type as a content type, this should be sufficient (i.e. no charset).
          validationService.validateXML(messageType, request.body).map {
            case Left(value) =>
              BadRequest(Json.toJson(FailedValidationResponse(value.toList))) // TODO: Fix validation response error (which will be a 400 probably?)
            case Right(_) => Ok(Json.toJson(SuccessfulValidationResponse))
          }
        case Some(x) =>
          request.body.runWith(Sink.ignore)
          Future.successful(
            UnsupportedMediaType(Json.toJson(UnsupportedMediaTypeError(s"Content type $x is not supported.").asInstanceOf[TransitMovementError]))
          )
        case None =>
          request.body.runWith(Sink.ignore)
          Future.successful(UnsupportedMediaType(Json.toJson(UnsupportedMediaTypeError(s"Content type must be specified.").asInstanceOf[TransitMovementError])))
      }
  }

}
