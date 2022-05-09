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

package uk.gov.hmrc.transitmovementsvalidator.models.response

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.__
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ErrorCode

object ValidationResponse {

  implicit lazy val failedValidationResponseWrites: OWrites[FailedValidationResponse] =
      (__ \ "validationErrors")
        .lazyWrite(OWrites.seq[String])
        .contramap {
          failedResponse: FailedValidationResponse =>
            failedResponse.validationErrors
        }
        .transform {
          converted: JsObject =>
            converted ++ failedJsonFragment
        }
  implicit lazy val successValidationResponseWrites: OWrites[SuccessfulValidationResponse.type] = OWrites {
    _ => successJson
  }

  private lazy val successJson: JsObject = Json.obj("success" -> true)
  private lazy val failedJsonFragment: JsObject = Json.obj(
    "success" -> false,
    "code"    -> ErrorCode.BadRequest,
    "message" -> "Failed to validate object")

  implicit lazy val validationResponseWrites: OWrites[ValidationResponse] = OWrites {
    case SuccessfulValidationResponse  => successJson
    case err: FailedValidationResponse => failedValidationResponseWrites.writes(err)
  }

}

sealed trait ValidationResponse
case object SuccessfulValidationResponse extends ValidationResponse
case class FailedValidationResponse(validationErrors: Seq[String]) extends ValidationResponse
