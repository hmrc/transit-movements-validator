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
