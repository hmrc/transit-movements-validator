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
import akka.stream.scaladsl.{Sink, Source, StreamConverters}
import akka.util.ByteString
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all._
import com.google.inject.ImplementedBy
import org.xml.sax.{Attributes, ErrorHandler, InputSource, SAXParseException}
import org.xml.sax.helpers.DefaultHandler
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.{BusinessValidationError, UnknownMessageType, XmlFailedValidation}
import uk.gov.hmrc.transitmovementsvalidator.models.errors.{ValidationError, XmlSchemaValidationError}

import java.io.InputStream
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Using

@ImplementedBy(classOf[XmlValidationServiceImpl])
trait XmlValidationService extends ValidationService

class XmlValidationServiceImpl @Inject() (implicit ec: ExecutionContext) extends XmlValidationService {

  private val pattern = raw"^IE(\d{3})".r

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
                  val parser = saxParser.newSAXParser.getParser

                  val errorBuffer: mutable.ListBuffer[XmlSchemaValidationError] =
                    new mutable.ListBuffer[XmlSchemaValidationError]

                  parser.setErrorHandler(new ErrorHandler {
                    override def warning(error: SAXParseException): Unit = {}

                    override def error(error: SAXParseException): Unit =
                      errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)

                    override def fatalError(error: SAXParseException): Unit =
                      errorBuffer += XmlSchemaValidationError.fromSaxParseException(error)
                  })

                  val inputSource = new InputSource(xmlInput)

                  val parseXml = Either
                    .catchOnly[SAXParseException] {
                      parser.parse(inputSource)
                    }
                    .leftMap {
                      exc =>
                        XmlFailedValidation(NonEmptyList.of(XmlSchemaValidationError.fromSaxParseException(exc)))
                    }

                  NonEmptyList
                    .fromList(errorBuffer.toList)
                    .map(
                      x => Either.left(XmlFailedValidation(x))
                    )
                    .getOrElse(parseXml)
              }.toEither
                .leftMap(
                  thr => ValidationError.Unexpected(Some(thr))
                )
                .flatten
          }
      }
    }

  override def businessRuleValidation(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit] =
    EitherT {
      val parser = MessageType
        .find(messageType)
        .map(
          t => parsersByType(t)
        )

      lazy val OfficeOfDeparture                 = "OfficeOfDeparture"
      lazy val OfficeOfDestinationActual         = "OfficeOfDestinationActual"
      lazy val CustomsOfficeOfEnquiryAtDeparture = "CustomsOfficeOfEnquiryAtDeparture"
      lazy val CustomsOfficeOfDeparture          = "CustomsOfficeOfDeparture"
      lazy val CustomsOfficeOfDestinationActual  = "CustomsOfficeOfDestinationActual"

      def checkMessageType(expectedMessageType: String): Option[String] =
        MessageType.find(expectedMessageType) match {
          case Some(msgType) if MessageType.departureValues.exists(_.rootNode.equalsIgnoreCase(expectedMessageType)) =>
            Some(OfficeOfDeparture)
          case Some(msgType) if MessageType.arrivalValues.exists(_.rootNode.equalsIgnoreCase(expectedMessageType)) =>
            Some(OfficeOfDestinationActual)
          case _ =>
            None
        }

      def validateReferenceNumber(referenceNumber: String, currentParentElement: Option[String]): Either[ValidationError, Unit] =
        currentParentElement match {
          case Some(parentElement) if !referenceNumber.toUpperCase.startsWith("GB") && !referenceNumber.toUpperCase.startsWith("XI") =>
            Left(BusinessValidationError(s"Did not recognise office:$parentElement"))
          case _ =>
            Right(())
        }

      pattern
        .findFirstMatchIn(messageType)
        .map {
          r =>
            val expectedMessageType = s"CC${r.group(1)}C"
            parser match {
              case None =>
                source.runWith(Sink.ignore)
                Future.successful(Left(UnknownMessageType(messageType)))
              case Some(futureType) =>
                futureType.map {
                  saxParser =>
                    val result: Either[ValidationError, Either[ValidationError, Unit]] =
                      Using[InputStream, Either[ValidationError, Unit]](source.runWith(StreamConverters.asInputStream(20.seconds))) {
                        xmlInput =>
                          val inputSource                          = new InputSource(xmlInput)
                          var elementValue                         = ""
                          var validationErrors                     = new ListBuffer[ValidationError]()
                          var currentParentElement: Option[String] = None
                          var referenceNumber: String              = ""

                          saxParser.newSAXParser.parse(
                            inputSource,
                            new DefaultHandler {
                              var inMessageTypeElement        = false
                              var withinCustomsOfficeElements = false

                              override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
                                if (inMessageTypeElement) {
                                  elementValue = new String(ch, start, length)
                                }
                                if (withinCustomsOfficeElements && currentParentElement.contains("referenceNumber")) {
                                  referenceNumber = new String(ch, start, length)
                                }
                              }

                              override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
                                if (qName == "messageType") {
                                  inMessageTypeElement = true
                                }

                                if (
                                  qName == CustomsOfficeOfDeparture || qName == CustomsOfficeOfDestinationActual || qName == CustomsOfficeOfEnquiryAtDeparture
                                ) {
                                  withinCustomsOfficeElements = true
                                }

                                if (withinCustomsOfficeElements && qName != "referenceNumber") {
                                  currentParentElement = Some(qName)
                                }
                              }

                              override def endElement(uri: String, localName: String, qName: String): Unit = {
                                if (qName == "messageType") {
                                  inMessageTypeElement = false
                                }

                                if (withinCustomsOfficeElements && qName == "referenceNumber") {
                                  val validationResult = validateReferenceNumber(referenceNumber, currentParentElement)
                                  validationResult match {
                                    case Left(error) =>
                                      validationErrors += error
                                    case _ => // No validation error
                                  }
                                }

                                if (
                                  qName == CustomsOfficeOfDeparture || qName == CustomsOfficeOfDestinationActual || qName == CustomsOfficeOfEnquiryAtDeparture
                                ) {
                                  withinCustomsOfficeElements = false
                                }
                              }
                            }
                          )

                          if (!elementValue.equalsIgnoreCase(expectedMessageType)) {
                            Left(BusinessValidationError("Root node doesn't match with the messageType"))
                          } else if (validationErrors.nonEmpty) {
                            Left(validationErrors.head)
                          } else {
                            Right(())
                          }
                      }.toEither.leftMap {
                        thr =>
                          ValidationError.Unexpected(Some(thr))
                      }

                    result.flatten
                }
            }
        }
        .getOrElse(Future.successful(Left[ValidationError, Unit](ValidationError.UnknownMessageType(messageType))))
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
