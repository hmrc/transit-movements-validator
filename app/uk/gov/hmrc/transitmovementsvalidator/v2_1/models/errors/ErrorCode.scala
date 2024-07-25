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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors

import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.OK
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.json.JsString
import play.api.libs.json.Writes

/** Common error codes documented in [[https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide#errors Developer Hub Reference Guide]]
  */
object ErrorCode {
  val BadRequest: ErrorCode              = ErrorCode("BAD_REQUEST", BAD_REQUEST)
  val NotFound: ErrorCode                = ErrorCode("NOT_FOUND", NOT_FOUND)
  val InternalServerError: ErrorCode     = ErrorCode("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR)
  val SchemaValidation: ErrorCode        = ErrorCode("SCHEMA_VALIDATION", OK)
  val UnsupportedMediaType: ErrorCode    = ErrorCode("UNSUPPORTED_MEDIA_TYPE", UNSUPPORTED_MEDIA_TYPE)
  val BusinessValidationError: ErrorCode = ErrorCode("BUSINESS_VALIDATION_ERROR", OK)

  implicit val errorCodeWrites: Writes[ErrorCode] = Writes {
    errorCode => JsString(errorCode.value)
  }
}

case class ErrorCode private (value: String, statusCode: Int) extends Product with Serializable
