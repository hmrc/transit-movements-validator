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

package uk.gov.hmrc.transitmovementsvalidator.services

import cats.data.EitherT
import cats.data.NonEmptyList
import cats.syntax.all._
import com.google.inject.ImplementedBy
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.StreamConverters
import org.apache.pekko.util.ByteString
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import play.api.Logging
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.XmlFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError

import java.io.InputStream
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.xml.XMLReader

@ImplementedBy(classOf[XmlValidationServiceImpl])
trait XmlValidationService extends ValidationService

class XmlValidationServiceImpl @Inject() (implicit ec: ExecutionContext) extends XmlValidationService with Logging {

  private lazy val parsersByType: Map[MessageType, Future[SAXParserFactory]] =
    MessageType.values.map {
      typ =>
        typ -> Future(buildParser(typ))
    }.toMap

  override def validate(messageType: MessageType, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      for {
        saxParser <- parsersByType(messageType)
        inputStream           = source.runWith(StreamConverters.asInputStream(20.seconds))
        (parser, errorBuffer) = createParser(saxParser)
        parsedXml             = parseXml(parser, inputStream)
        result                = transformFailures(parsedXml, errorBuffer)
      } yield result
    }

  private def transformFailures(
    parsedXml: Either[XmlFailedValidation, Unit],
    errorBuffer: ListBuffer[XmlSchemaValidationError]
  ): Either[XmlFailedValidation, Unit] =
    NonEmptyList
      .fromList(errorBuffer.toList)
      .map(
        x => Either.left(XmlFailedValidation(x))
      )
      .getOrElse(parsedXml)

  private def parseXml(parser: XMLReader, inputStream: InputStream): Either[XmlFailedValidation, Unit] = {
    val inputSource = new InputSource(inputStream)
    Either
      .catchOnly[SAXParseException] {
        parser.parse(inputSource)
      }
      .leftMap {
        exc =>
          XmlFailedValidation(NonEmptyList.of(XmlSchemaValidationError.fromSaxParseException(exc)))
      }
  }

  private def createParser(saxParser: SAXParserFactory): (XMLReader, ListBuffer[XmlSchemaValidationError]) = {
    val parser      = saxParser.newSAXParser.getXMLReader
    val errorBuffer = new mutable.ListBuffer[XmlSchemaValidationError]

    parser.setErrorHandler(new ErrorHandler {
      override def warning(error: SAXParseException): Unit = {}

      override def error(error: SAXParseException): Unit =
        errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)

      override def fatalError(error: SAXParseException): Unit =
        errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)
    })

    (parser, errorBuffer)
  }

  private def buildParser(messageType: MessageType): SAXParserFactory = {
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

}
