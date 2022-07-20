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
    case sve: XmlSchemaValidationError         => Json.toJsObject(sve)
    case jve: JsonSchemaValidationError        => Json.toJsObject(jve)
  }
}

case class UnknownMessageTypeValidationError(message: String) extends ValidationError

object UnknownMessageTypeValidationError {
  implicit val format: OFormat[UnknownMessageTypeValidationError] = Json.format[UnknownMessageTypeValidationError]
}

case class JsonSchemaValidationError(path: String, schemaPath: String, message: String) extends ValidationError

object JsonSchemaValidationError {

  implicit val jsonValidationErrorReads: Reads[JsonSchemaValidationError] =
    (
      (__ \ "path").read[String] and
        (__ \ "schemaPath").read[String] and
        (__ \ "message").read[String]
    )(JsonSchemaValidationError.apply _)

  implicit val jsonValidationErrorWrites: OWrites[JsonSchemaValidationError] =
    (
      (__ \ "path").write[String] and
        (__ \ "schemaPath").write[String] and
        (__ \ "message").write[String]
    )(unlift(JsonSchemaValidationError.unapply))
}

case class XmlSchemaValidationError(lineNumber: Int, columnNumber: Int, message: String) extends ValidationError

object XmlSchemaValidationError {

  def fromSaxParseException(ex: SAXParseException) =
    XmlSchemaValidationError(ex.getLineNumber, ex.getColumnNumber, ex.getMessage)

  implicit val schemaValidationErrorReads: Reads[XmlSchemaValidationError] =
    (
      (__ \ "lineNumber").read[Int] and
        (__ \ "columnNumber").read[Int] and
        (__ \ "message").read[String]
    )(XmlSchemaValidationError.apply _)

  implicit val schemaValidationErrorWrites: OWrites[XmlSchemaValidationError] =
    (
      (__ \ "lineNumber").write[Int] and
        (__ \ "columnNumber").write[Int] and
        (__ \ "message").write[String]
    )(unlift(XmlSchemaValidationError.unapply))
}
