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
import cats.syntax.all._
import com.google.inject.ImplementedBy
import org.xml.sax.Attributes
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.UnknownMessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.XmlFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError

import java.io.InputStream
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Using
import scala.xml.SAXException

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

      val CustomsOfficeOfEnquiryAtDeparture = "CustomsOfficeOfEnquiryAtDeparture"
      val CustomsOfficeOfDeparture          = "CustomsOfficeOfDeparture"
      val CustomsOfficeOfDestinationActual  = "CustomsOfficeOfDestinationActual"

      def validateReferenceNumber(referenceNumber: String, currentParentElement: Option[String]): Either[ValidationError, Unit] =
        currentParentElement match {
          case Some(parentElement)
              if parentElement == CustomsOfficeOfDestinationActual || parentElement == CustomsOfficeOfDeparture || parentElement == CustomsOfficeOfEnquiryAtDeparture =>
            if (!referenceNumber.toUpperCase.startsWith("GB") && !referenceNumber.toUpperCase.startsWith("XI")) {
              Left(BusinessValidationError(s"Invalid reference number: $referenceNumber"))
            } else {
              Right(())
            }
          case _ =>
            Right(()) // Skip validation for other parent elements
        }

      pattern
        .findFirstMatchIn(messageType)
        .map(
          r => s"CC${r.group(1)}C"
        )
        .map {
          expectedMessageType =>
            parser match {
              case None =>
                //Must be run to prevent memory overflow (the request *MUST* be consumed somehow)
                source.runWith(Sink.ignore)
                Future.successful(Left(UnknownMessageType(messageType)))
              case Some(futureType) =>
                futureType.map {
                  saxParser =>
                    // Unfortunately, we need to extract this out to help the compiler,
                    // without it, it can't infer the types for flatten.
                    // Probably due to the fact that there is nesting and the compiler gets confused.
                    val result: Either[ValidationError, Either[ValidationError, Unit]] =
                      Using[InputStream, Either[ValidationError, Unit]](source.runWith(StreamConverters.asInputStream(20.seconds))) {
                        xmlInput =>
                          try {
                            val inputSource                          = new InputSource(xmlInput)
                            var elementValue                         = ""
                            var currentParentElement: Option[String] = None
                            var referenceNumber: String              = ""

                            saxParser.newSAXParser.parse(
                              inputSource,
                              new DefaultHandler {
                                var inMessageTypeElement     = false
                                var inReferenceNumberElement = false

                                override def characters(ch: Array[Char], start: Int, length: Int): Unit =
                                  if (inMessageTypeElement) {
                                    elementValue = new String(ch, start, length)
                                  } else if (inReferenceNumberElement) {
                                    referenceNumber = new String(ch, start, length)
                                  }

                                override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
                                  inMessageTypeElement = qName.equals("messageType")
                                  inReferenceNumberElement = qName.equals("referenceNumber")
                                  if (
                                    qName.equals(CustomsOfficeOfEnquiryAtDeparture) ||
                                    qName.equals(CustomsOfficeOfDeparture) ||
                                    qName.equals(CustomsOfficeOfDestinationActual)
                                  ) {
                                    currentParentElement = Some(qName)
                                  }
                                }

                                override def endElement(uri: String, localName: String, qName: String): Unit = {
                                  inMessageTypeElement = false
                                  if (qName.equals("referenceNumber")) {
                                    inReferenceNumberElement = false
                                    val validationResult = validateReferenceNumber(referenceNumber, currentParentElement)
                                    validationResult match {
                                      case Left(error) =>
                                        error match {
                                          case BusinessValidationError(message) => throw new SAXException(message)
                                          case UnknownMessageType(messageType)  => throw new SAXException(s"Unknown message type: $messageType")
                                          case XmlFailedValidation(errors)      => throw new SAXException(errors.toList.map(_.message).mkString(", "))
                                        }
                                      case _ => // No validation error
                                    }
                                  } else if (
                                    qName.equals(CustomsOfficeOfEnquiryAtDeparture) ||
                                    qName.equals(CustomsOfficeOfDeparture) ||
                                    qName.equals(CustomsOfficeOfDestinationActual)
                                  ) {
                                    currentParentElement = None
                                  }
                                }

                              }
                            )

                            if (!elementValue.equalsIgnoreCase(expectedMessageType)) {
                              Left(BusinessValidationError("Root node doesn't match with the messageType"))
                            } else {
                              Right(())
                            }
                          } catch {
                            case e: SAXException => Left(BusinessValidationError(e.getMessage))
                          }
                      }.toEither.leftMap {
                        thr => ValidationError.Unexpected(Some(thr))
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
