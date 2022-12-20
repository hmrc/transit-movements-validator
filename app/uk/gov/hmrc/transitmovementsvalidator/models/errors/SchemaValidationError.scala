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
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import play.api.libs.json.__

object SchemaValidationError {

  implicit val schemaValidationErrorWrites: OWrites[SchemaValidationError] = OWrites {
    case x: XmlSchemaValidationError  => xmlSchemaValidationErrorWrites.writes(x)
    case x: JsonSchemaValidationError => jsonValidationErrorWrites.writes(x)
  }

  implicit val jsonValidationErrorReads: Reads[JsonSchemaValidationError] =
    (
      (__ \ "schemaPath").read[String] and
        (__ \ "message").read[String]
    )(JsonSchemaValidationError.apply _)

  implicit val jsonValidationErrorWrites: OWrites[JsonSchemaValidationError] =
    (
      (__ \ "schemaPath").write[String] and
        (__ \ "message").write[String]
    )(unlift(JsonSchemaValidationError.unapply))

  implicit val xmlSchemaValidationErrorReads: Reads[XmlSchemaValidationError] =
    (
      (__ \ "lineNumber").read[Int] and
        (__ \ "columnNumber").read[Int] and
        (__ \ "message").read[String]
    )(XmlSchemaValidationError.apply _)

  implicit val xmlSchemaValidationErrorWrites: OWrites[XmlSchemaValidationError] =
    (
      (__ \ "lineNumber").write[Int] and
        (__ \ "columnNumber").write[Int] and
        (__ \ "message").write[String]
    )(unlift(XmlSchemaValidationError.unapply))

}

sealed trait SchemaValidationError

case class JsonSchemaValidationError(schemaPath: String, message: String) extends SchemaValidationError

case class XmlSchemaValidationError(lineNumber: Int, columnNumber: Int, message: String) extends SchemaValidationError

object XmlSchemaValidationError {

  def fromSaxParseException(ex: SAXParseException) =
    XmlSchemaValidationError(ex.getLineNumber, ex.getColumnNumber, ex.getMessage)

}
