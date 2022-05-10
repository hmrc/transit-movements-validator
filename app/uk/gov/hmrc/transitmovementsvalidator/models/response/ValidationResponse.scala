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

import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Writes
import play.api.libs.json.__

object ValidationResponse {
  private val validationErrorsFieldName = "validationErrors"
  private val successJson               = Json.obj(validationErrorsFieldName -> Json.arr())

  private val failedValidationResponseWrites: OWrites[FailedValidationResponse] =
    (__ \ validationErrorsFieldName).lazyWrite(Writes.seq[String]).contramap(_.validationErrors)

  implicit val validationResponseWrites: Writes[ValidationResponse] = OWrites {
    case SuccessfulValidationResponse       => successJson
    case response: FailedValidationResponse => failedValidationResponseWrites.writes(response)
  }
}

sealed trait ValidationResponse {
  def validationErrors: Seq[String] // TODO: Replace with actual error type
}

object SuccessfulValidationResponse extends ValidationResponse {
  val validationErrors: Seq[String] = Seq.empty[String]
}
case class FailedValidationResponse(validationErrors: Seq[String]) extends ValidationResponse
