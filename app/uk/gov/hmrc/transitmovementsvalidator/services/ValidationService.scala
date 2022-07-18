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

package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import cats.data.NonEmptyList
import com.google.inject.ImplementedBy
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import cats.syntax.all._
import com.eclipsesource.schema.SchemaType
import com.eclipsesource.schema.SchemaValidator
import com.eclipsesource.schema.drafts.Version4.schemaTypeReads
import com.eclipsesource.schema.drafts.Version7
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import javax.inject.Inject
import scala.collection.mutable

@ImplementedBy(classOf[ValidationServiceImpl])
trait ValidationService {

  def validateXML(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]]

  def validateJSON(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]]
}

class ValidationServiceImpl @Inject() extends ValidationService with XmlValidation {

  override def validateXML(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]] =
    MessageType.values.find(_.code == messageType) match {
      case None =>
        //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
        source.runWith(Sink.ignore)
        Future.successful(Left(NonEmptyList.one(ValidationError.fromUnrecognisedMessageType(messageType))))
      case Some(mType) =>
        parsersByType(mType).map {
          saxParser =>
            val parser = saxParser.newSAXParser.getParser

            val errorBuffer: mutable.ListBuffer[SchemaValidationError] =
              new mutable.ListBuffer[SchemaValidationError]

            parser.setErrorHandler(new ErrorHandler {
              override def warning(error: SAXParseException): Unit = {}

              override def error(error: SAXParseException): Unit =
                errorBuffer += SchemaValidationError.fromSaxParseException(error)

              override def fatalError(error: SAXParseException): Unit =
                errorBuffer += SchemaValidationError.fromSaxParseException(error)
            })

            val xmlInput = source.runWith(StreamConverters.asInputStream(20.seconds))

            val inputSource = new InputSource(xmlInput)

            val parseXml = Either
              .catchOnly[SAXParseException] {
                parser.parse(inputSource)
              }
              .leftMap {
                exc =>
                  NonEmptyList.of(SchemaValidationError.fromSaxParseException(exc))
              }

            NonEmptyList
              .fromList(errorBuffer.toList)
              .map(Either.left)
              .getOrElse(parseXml)
        }
    }

  override def validateJSON(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Future[Either[NonEmptyList[ValidationError], Unit]] =
    MessageType.values.find(_.code == messageType) match {
      case None =>
        //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
        source.runWith(Sink.ignore)
        Future.successful(Left(NonEmptyList.one(ValidationError.fromUnrecognisedMessageType(messageType))))
      case Some(mType) =>
        val validator = SchemaValidator(Some(Version7))
        val schema = Json
          .fromJson[SchemaType](Json.parse("""json_schema""".stripMargin))
          .get

        val result = source
          .map(_.utf8String)
          .map(Json.parse)
          .map(validator.validate(schema, _))
          .runWith(Sink.head[JsResult[JsValue]])

        result.map {
          case JsError(errors) =>
            errors.map {
              e =>
                val messages = e._2.map(
                  error => error.message
                )
                ValidationError(s"Error in path ${e._1}: $messages")
            }
            val validationErrors = NonEmptyList.fromList(errors.toList).get
            Left(validationErrors)
          case JsSuccess(_, _) => Right(())
        }

    }

}
