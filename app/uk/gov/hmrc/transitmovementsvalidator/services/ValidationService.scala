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
import com.google.inject.Singleton
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import cats.syntax.all._
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import scala.collection.mutable

@ImplementedBy(classOf[ValidationServiceImpl])
trait ValidationService {

  def validateXML(messageType: String, source: Source[ByteString, _])(implicit materializer: Materializer): Future[Either[NonEmptyList[ValidationError], Unit]]

}

class ValidationServiceImpl @Inject() (implicit ec: ExecutionContext) extends ValidationService {

  val parsersByType: Map[MessageType, Future[SAXParserFactory]] =
    MessageType.values.map {
      typ =>
        typ -> Future(buildParser(typ))
    }.toMap

  def buildParser(messageType: MessageType): SAXParserFactory = {
    val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    val parser        = SAXParserFactory.newInstance()
    val schemaUrl     = getClass.getResource(messageType.xsdPath)

    val schema = schemaFactory.newSchema(schemaUrl)
    parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    parser.setFeature("http://xml.org/sax/features/external-general-entities", false)
    parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    parser.setNamespaceAware(true)
    parser.setXIncludeAware(false)
    parser.setSchema(schema)

    parser
  }

  override def validateXML(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer
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

}
