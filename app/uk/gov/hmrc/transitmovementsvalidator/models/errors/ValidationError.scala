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

import org.xml.sax.SAXParseException
import play.api.libs.json.{Json, OFormat}

sealed trait ValidationError {
  def message: String
}

object ValidationError {

  final case class UnknownMessageTypeValidationError(message: String) extends ValidationError

  def fromUnrecognisedMessageType(s: String): UnknownMessageTypeValidationError =
    UnknownMessageTypeValidationError(s"Unknown Message Type provided: $s is not recognised")

  implicit val validationError: OFormat[ValidationError] =
    Json.format[ValidationError]

}

case class SchemaValidationError(lineNumber: Int, columnNumber: Int, message: String) extends ValidationError

object SchemaValidationError {
  def fromSaxParseException(ex: SAXParseException) =
    SchemaValidationError(ex.getLineNumber, ex.getColumnNumber, ex.getMessage)

  implicit val schemaValidationError: OFormat[SchemaValidationError] =
    Json.format[SchemaValidationError]
}
