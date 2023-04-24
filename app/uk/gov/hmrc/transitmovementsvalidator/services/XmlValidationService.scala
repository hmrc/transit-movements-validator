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

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import cats.data.EitherT
import cats.data.NonEmptyList
import com.google.inject.ImplementedBy
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import cats.syntax.all._
import org.xml.sax.helpers.DefaultHandler
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.UnknownMessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.XmlFailedValidation

import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import scala.collection.mutable
import scala.util.Using

@ImplementedBy(classOf[XmlValidationServiceImpl])
trait XmlValidationService extends ValidationService

class XmlValidationServiceImpl @Inject() (implicit ec: ExecutionContext) extends XmlValidationService {

  private lazy val parsersByType: Map[MessageType, Future[SAXParserFactory]] =
    MessageType.values.map {
      typ =>
        typ -> Future(buildParser(typ))
    }.toMap

  override def validate(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      val parser = MessageType
        .find(messageType)
        .map(
          t => parsersByType(t)
        )
      parser match {
        case None =>
          //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
          source.runWith(Sink.ignore)
          Future.successful(Left(UnknownMessageType(messageType)))
        case Some(futureType) =>
          futureType.map {
            saxParser =>
              Using(source.runWith(StreamConverters.asInputStream(20.seconds))) {
                xmlInput =>
                  val inputSource = new InputSource(xmlInput)
                  val errorBuffer: mutable.ListBuffer[XmlSchemaValidationError] =
                    new mutable.ListBuffer[XmlSchemaValidationError]
                  var elementValue = ""
                  var rootTag      = ""

                  val parser = saxParser.newSAXParser.parse(
                    inputSource,
                    new DefaultHandler {
                      var inMessageTypeElement = false

                      override def characters(ch: Array[Char], start: Int, length: Int): Unit =
                        if (inMessageTypeElement) {
                          elementValue = new String(ch, start, length)
                        }

                      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = {
                        if (qName.startsWith("ncts:")) {
                          rootTag = localName
                        }
                        if (qName.equals("messageType")) {
                          inMessageTypeElement = true
                        }
                      }

                      override def endElement(uri: String, localName: String, qName: String): Unit =
                        if (qName.equals("messageType")) {
                          inMessageTypeElement = false
                        }

                      override def warning(error: SAXParseException): Unit = {}

                      override def error(error: SAXParseException): Unit =
                        errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)

                      override def fatalError(error: SAXParseException): Unit =
                        errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)
                    }
                  )

                  val parseXml = Either
                    .catchOnly[SAXParseException] {
                      parser
                    }
                    .leftMap {
                      exc =>
                        XmlFailedValidation(NonEmptyList.of(XmlSchemaValidationError.fromSaxParseException(exc)))
                    }

                  if (!elementValue.equalsIgnoreCase(rootTag)) {
                    Either.left(BusinessValidationError("Root node doesn't match with the messageType"))
                  } else {
                    NonEmptyList
                      .fromList(errorBuffer.toList)
                      .map(
                        x => Either.left(XmlFailedValidation(x))
                      )
                      .getOrElse(parseXml)
                  }

              }.toEither
                .leftMap(
                  thr => ValidationError.Unexpected(Some(thr))
                )
                .flatten
          }
      }
    }

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

}
