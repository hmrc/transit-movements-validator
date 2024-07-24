/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsvalidator.routing

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.controllers.{MessagesController => TransitionalMessagesController}
import uk.gov.hmrc.transitmovementsvalidator.models.Final
import uk.gov.hmrc.transitmovementsvalidator.models.Transitional
import uk.gov.hmrc.transitmovementsvalidator.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsvalidator.v2_1.controllers.{MessagesController => FinalMessagesController}

import javax.inject.Inject

class VersionedRoutingController @Inject() (
  cc: ControllerComponents,
  transitionalController: TransitionalMessagesController,
  finalController: FinalMessagesController
)(implicit val materializer: Materializer, appConfig: AppConfig)
    extends BackendController(cc)
    with StreamingParsers
    with VersionedRouting {

  def validate(messageType: String): Action[Source[ByteString, _]] =
    apiVersionRoute {
      case Some(Final.value) =>
        finalController.validate(messageType)
      case Some(Transitional.value) =>
        transitionalController.validate(messageType)
    }
}
