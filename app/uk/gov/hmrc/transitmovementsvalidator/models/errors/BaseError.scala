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

package uk.gov.hmrc.transitmovementsvalidator.models.errors

import cats.data.NonEmptyList
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.OWrites
import play.api.libs.json.__
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.transitmovementsvalidator.models.formats.CommonFormats

object BaseError extends CommonFormats {

  val MessageFieldName = "message"
  val CodeFieldName    = "code"

  def forbiddenError(message: String): BaseError =
    StandardError(message, ErrorCode.Forbidden)

  def entityTooLargeError(message: String): BaseError =
    StandardError(message, ErrorCode.EntityTooLarge)

  def unsupportedMediaTypeError(message: String): BaseError =
    StandardError(message, ErrorCode.UnsupportedMediaType)

  def badRequestError(message: String): BaseError =
    StandardError(message, ErrorCode.BadRequest)

  def notFoundError(message: String): BaseError =
    StandardError(message, ErrorCode.NotFound)

  // TODO: change String to more appropriate type
  def schemaValidationError(errors: NonEmptyList[String]): BaseError =
    SchemaValidationError("Failed to validate object", ErrorCode.SchemaValidation, errors)

  def upstreamServiceError(
    message: String = "Internal server error",
    code: ErrorCode = ErrorCode.InternalServerError,
    cause: UpstreamErrorResponse
  ): BaseError =
    UpstreamServiceError(message, code, cause)

  def internalServiceError(
    message: String = "Internal server error",
    code: ErrorCode = ErrorCode.InternalServerError,
    cause: Option[Throwable] = None
  ): BaseError =
    InternalServiceError(message, code, cause)

  def unapply(error: BaseError): Option[(String, ErrorCode)] = Some((error.message, error.code))

  private val fallbackTransitMovementErrorWrites: OWrites[BaseError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ CodeFieldName).write[ErrorCode]
    )(unlift(BaseError.unapply))

  implicit val schemaValidationTransitMovementErrorWrites: OWrites[SchemaValidationError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ CodeFieldName).write[ErrorCode] and
        (__ \ "validationErrors").write[NonEmptyList[String]]
    )(unlift(SchemaValidationError.unapply))

  implicit val transitMovementErrorWrites: OWrites[BaseError] = {
    case err: SchemaValidationError => schemaValidationTransitMovementErrorWrites.writes(err)
    case err                        => fallbackTransitMovementErrorWrites.writes(err)
  }
}

sealed abstract class BaseError extends Product with Serializable {
  def message: String
  def code: ErrorCode
}

case class StandardError(message: String, code: ErrorCode) extends BaseError

case class SchemaValidationError(message: String, code: ErrorCode, validationErrors: NonEmptyList[String]) extends BaseError

case class UpstreamServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: UpstreamErrorResponse
) extends BaseError

object UpstreamServiceError {

  def causedBy(cause: UpstreamErrorResponse): BaseError =
    BaseError.upstreamServiceError(cause = cause)
}

case class InternalServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: Option[Throwable] = None
) extends BaseError

object InternalServiceError {

  def causedBy(cause: Throwable): BaseError =
    BaseError.internalServiceError(cause = Some(cause))
}