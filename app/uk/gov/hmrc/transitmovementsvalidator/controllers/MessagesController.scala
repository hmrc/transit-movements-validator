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
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse

import javax.inject.Inject
import scala.concurrent.Future

class MessagesController @Inject() (cc: ControllerComponents)
                                   (implicit val materializer: Materializer)
  extends BackendController(cc) with StreamingParsers {

  def validate(messageType: String): Action[Source[ByteString, _]] = Action.async(streamFromMemory) {
    implicit request =>
      request.body.runWith(Sink.ignore) // call into the service instead
      Future.successful(Ok(Json.toJson(ValidationResponse(true))))
  }

}
