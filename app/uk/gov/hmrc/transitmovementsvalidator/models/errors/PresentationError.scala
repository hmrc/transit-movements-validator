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

package uk.gov.hmrc.transitmovementsvalidator.models.errors

import cats.data.NonEmptyList
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.OWrites
import play.api.libs.json.__
import uk.gov.hmrc.transitmovementsvalidator.utils.NonEmptyListFormat._

object PresentationError {

  val MessageFieldName = "message"
  val CodeFieldName    = "code"

  def unapply(pe: PresentationError): Option[(String, ErrorCode)] = Some((pe.message, pe.code))

  val standardPresentationErrorWrites: OWrites[PresentationError] = (
    (__ \ MessageFieldName).write[String] and
      (__ \ CodeFieldName).write[ErrorCode]
  )(unlift(PresentationError.unapply))

  implicit val presentationErrorWrites: OWrites[PresentationError] = OWrites {
    case x: SchemaValidationPresentationError => SchemaValidationPresentationError.writes.writes(x)
    case x: PresentationError                 => standardPresentationErrorWrites.writes(x)
  }

  def badRequestError(message: String): PresentationError =
    StandardError(message, ErrorCode.BadRequest)

  def notFoundError(message: String): PresentationError =
    StandardError(message, ErrorCode.NotFound)

  def notAcceptableError(message: String): PresentationError =
    StandardError(message, ErrorCode.NotAcceptable)

  def unsupportedMediaTypeError(message: String): PresentationError =
    StandardError(message, ErrorCode.UnsupportedMediaType)

  def schemaValidationError(errors: NonEmptyList[SchemaValidationError]): PresentationError =
    SchemaValidationPresentationError(errors)

  def internalServiceError(
    message: String = "Internal server error",
    code: ErrorCode = ErrorCode.InternalServerError,
    cause: Option[Throwable] = None
  ): PresentationError =
    InternalServiceError(message, code, cause)

  def businessValidationError(message: String): PresentationError =
    StandardError(message, ErrorCode.BusinessValidationError)

}

sealed abstract class PresentationError extends Product with Serializable {
  def message: String
  def code: ErrorCode
}

case class StandardError(message: String, code: ErrorCode) extends PresentationError

object InternalServiceError {

  def causedBy(cause: Throwable): PresentationError =
    InternalServiceError(cause = Some(cause))
}

case class InternalServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: Option[Throwable] = None
) extends PresentationError

object SchemaValidationPresentationError {

  implicit val writes: OWrites[SchemaValidationPresentationError] =
    (__ \ "errors").write[NonEmptyList[SchemaValidationError]].contramap(_.errors)
}

case class SchemaValidationPresentationError(errors: NonEmptyList[SchemaValidationError]) extends PresentationError {
  val message: String = "Schema Validation Error"
  val code: ErrorCode = ErrorCode.SchemaValidation
}
