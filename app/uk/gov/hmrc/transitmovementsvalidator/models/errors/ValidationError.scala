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
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import play.api.libs.json.__

sealed trait ValidationError {
  def message: String
}

object ValidationError {

  def fromUnrecognisedMessageType(s: String): UnknownMessageTypeValidationError =
    UnknownMessageTypeValidationError.apply(s"Unknown Message Type provided: $s is not recognised")

  implicit lazy val format: OFormat[ValidationError] = OFormat.oFormatFromReadsAndOWrites(reads, writes)

  implicit lazy val reads: Reads[ValidationError] = Json.reads[ValidationError]

  implicit lazy val writes: OWrites[ValidationError] = OWrites {
    case um: UnknownMessageTypeValidationError => Json.toJsObject(um)
    case sve: SchemaValidationError            => Json.toJsObject(sve)
  }
}

case class UnknownMessageTypeValidationError(message: String) extends ValidationError

object UnknownMessageTypeValidationError {
  implicit val format: OFormat[UnknownMessageTypeValidationError] = Json.format[UnknownMessageTypeValidationError]
}

case class SchemaValidationError(lineNumber: Int, columnNumber: Int, message: String) extends ValidationError

object SchemaValidationError {

  def fromSaxParseException(ex: SAXParseException) =
    SchemaValidationError(ex.getLineNumber, ex.getColumnNumber, ex.getMessage)

  implicit val schemaValidationErrorReads: Reads[SchemaValidationError] =
    (
      (__ \ "lineNumber").read[Int] and
        (__ \ "columnNumber").read[Int] and
        (__ \ "message").read[String]
    )(SchemaValidationError.apply _)

  implicit val schemaValidationErrorWrites: OWrites[SchemaValidationError] =
    (
      (__ \ "lineNumber").write[Int] and
        (__ \ "columnNumber").write[Int] and
        (__ \ "message").write[String]
    )(unlift(SchemaValidationError.unapply))
}
