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

sealed abstract class TransitMovementError extends Product with Serializable {
  def message: String
  def code: String
}

case class StandardTransitMovementError(message: String, code: String) extends TransitMovementError

case class SchemaValidationTransitMovementError(message: String, code: String, validationErrors: NonEmptyList[String]) extends TransitMovementError

case class UpstreamServiceError(
  message: String = "Internal server error",
  code: String = ErrorCode.InternalServerError,
  cause: UpstreamErrorResponse
) extends TransitMovementError

object UpstreamServiceError {

  def causedBy(cause: UpstreamErrorResponse): TransitMovementError =
    TransitMovementError.upstreamServiceError(cause = cause)
}

case class InternalServiceError(
  message: String = "Internal server error",
  code: String = ErrorCode.InternalServerError,
  cause: Option[Throwable] = None
) extends TransitMovementError

object InternalServiceError {

  def causedBy(cause: Throwable): TransitMovementError =
    TransitMovementError.internalServiceError(cause = Some(cause))
}

object TransitMovementError extends CommonFormats {

  val MessageFieldName = "message"

  def unapply(error: TransitMovementError): Option[(String, String)] = Some((error.message, error.code))

  private val fallbackTransitMovementErrorWrites: OWrites[TransitMovementError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ ErrorCode.FieldName).write[String]
    )(unlift(TransitMovementError.unapply))

  implicit val schemaValidationTransitMovementErrorWrites: OWrites[SchemaValidationTransitMovementError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ ErrorCode.FieldName).write[String] and
        (__ \ "validationErrors").write[NonEmptyList[String]]
    )(unlift(SchemaValidationTransitMovementError.unapply))

  implicit val transitMovementErrorWrites: OWrites[TransitMovementError] = {
    case err: SchemaValidationTransitMovementError => schemaValidationTransitMovementErrorWrites.writes(err)
    case err                                       => fallbackTransitMovementErrorWrites.writes(err)
  }

  def forbiddenError(message: String): TransitMovementError =
    StandardTransitMovementError(message, ErrorCode.Forbidden)

  def entityTooLargeError(message: String): TransitMovementError =
    StandardTransitMovementError(message, ErrorCode.EntityTooLarge)

  def unsupportedMediaTypeError(message: String): TransitMovementError =
    StandardTransitMovementError(message, ErrorCode.UnsupportedMediaType)

  def badRequestError(message: String): TransitMovementError =
    StandardTransitMovementError(message, ErrorCode.BadRequest)

  def notFoundError(message: String): TransitMovementError =
    StandardTransitMovementError(message, ErrorCode.NotFound)

  // TODO: change String to more appropriate type
  def schemaValidationError(errors: NonEmptyList[String]): TransitMovementError =
    SchemaValidationTransitMovementError("Failed to validate object", ErrorCode.SchemaValidation, errors)

  def upstreamServiceError(
    message: String = "Internal server error",
    code: String = ErrorCode.InternalServerError,
    cause: UpstreamErrorResponse
  ): TransitMovementError =
    UpstreamServiceError(message, code, cause)

  def internalServiceError(
    message: String = "Internal server error",
    code: String = ErrorCode.InternalServerError,
    cause: Option[Throwable] = None
  ): TransitMovementError =
    InternalServiceError(message, code, cause)
}
