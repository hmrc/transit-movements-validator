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
import org.xml.sax.ErrorHandler
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import cats.syntax.all._
import org.xml.sax.helpers.DefaultHandler
import uk.gov.hmrc.transitmovementsvalidator.models.ArrivalMessageType
import uk.gov.hmrc.transitmovementsvalidator.models.DepartureMessageType
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

      //This method is used within businessRuleValidation to determine the type of the message, and the business rules validation process
      //proceeds accordingly based on the type of message determined.
      //It splits the rootTag by ":", and uses the second part (at index 1) to find a matching MessageType from the set of all MessageType values.
      //The comparison is case-insensitive, as indicated by equalsIgnoreCase.
      //Depending on the type of MessageType it found:
      //If the MessageType is a subtype of DepartureMessageType and is included in the defined departure values, the function returns the string "OfficeOfDeparture".
      //If the MessageType is a subtype of ArrivalMessageType and is included in the defined arrival values, the function returns the string "OfficeOfDestinationActual".
      //For all other cases, the function returns None.
      def checkMessageType(rootTag: String): Option[String] = {
        val messageType = MessageType.values.find(_.rootNode.equalsIgnoreCase(rootTag.split(":")(1)))

        messageType match {
          case Some(msgType: DepartureMessageType) if MessageType.departureValues.contains(msgType) =>
            Some(OfficeOfDeparture)
          case Some(msgType: ArrivalMessageType) if MessageType.arrivalValues.contains(msgType) =>
            Some(OfficeOfDestinationActual)
          case _ =>
            None
        }
      }

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
                  val inputSource                        = new InputSource(xmlInput)
                  var elementValue                       = ""
                  var rootTag                            = ""
                  var referenceNumber                    = ""
                  var checkedMessageType                 = ""
                  var inReferenceNumber                  = false
                  var inCustomsOfficeOfDeparture         = false
                  var inCustomsOfficeOfDestinationActual = false
                  var withinCustomsOfficeElements        = false
                  var currentParentElement: String       = ""

                  saxParser.newSAXParser.parse(
                    inputSource,
                    new DefaultHandler {
                      var inMessageTypeElement                                             = false
                      var startPrefix                                                      = ""
                      var customsOfficeOfDestinationActualReferenceNumber: Option[String]  = None
                      var customsOfficeOfEnquiryAtDepartureReferenceNumber: Option[String] = None

                      var withinCustomsOfficeDestinationActual  = false
                      var withinCustomsOfficeEnquiryAtDeparture = false

                      override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
                        if (inMessageTypeElement) {
                          elementValue = new String(ch, start, length)
                        }
                        if (inReferenceNumber && withinCustomsOfficeElements) {
                          referenceNumber = new String(ch, start, length)
                        }
                      }

                      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
                        if (uri.nonEmpty) {
                          rootTag = qName
                          checkMessageType(rootTag) match {
                            case Some(msgType) => checkedMessageType = msgType
                            case None          => // Nothing happens
                          }
                        }
                        if (qName.equals("messageType")) {
                          inMessageTypeElement = true
                        }

                        if (checkedMessageType.equals(OfficeOfDeparture)) {

                          if (qName.equals(CustomsOfficeOfEnquiryAtDeparture) || qName.equals(CustomsOfficeOfDeparture)) {
                            inCustomsOfficeOfDeparture = true
                            withinCustomsOfficeElements = true
                            withinCustomsOfficeEnquiryAtDeparture = true
                          }

                        }

                        if (checkedMessageType.equals(OfficeOfDestinationActual)) {
                          if (qName.equals(CustomsOfficeOfDestinationActual)) {
                            inCustomsOfficeOfDestinationActual = true
                            withinCustomsOfficeDestinationActual = true
                            withinCustomsOfficeElements = true
                          }
                        }

                        if (withinCustomsOfficeElements && qName != "referenceNumber") {
                          currentParentElement = qName
                        }
                        if (qName.equals("referenceNumber")) {
                          inReferenceNumber = true
                        }
                      }

                      override def endElement(uri: String, localName: String, qName: String): Unit = {
                        if (qName.equals("messageType")) {
                          inMessageTypeElement = false
                        }

                        if (checkedMessageType.equals(OfficeOfDestinationActual)) {
                          if (qName.equals(CustomsOfficeOfDestinationActual)) {
                            inCustomsOfficeOfDestinationActual = false
                            withinCustomsOfficeDestinationActual = false
                            withinCustomsOfficeElements = false
                          }
                        }

                        if (checkedMessageType.equals(OfficeOfDeparture)) {

                          if (qName.equals(CustomsOfficeOfEnquiryAtDeparture) || qName.equals(CustomsOfficeOfDeparture)) {
                            inCustomsOfficeOfDeparture = false
                            withinCustomsOfficeElements = false
                            withinCustomsOfficeEnquiryAtDeparture = false
                          }

                        }

                        if (withinCustomsOfficeElements && qName.equals("referenceNumber")) {
                          inReferenceNumber = false

                          if (withinCustomsOfficeDestinationActual) {
                            customsOfficeOfDestinationActualReferenceNumber = Some(referenceNumber)
                          }

                          if (withinCustomsOfficeEnquiryAtDeparture) {
                            customsOfficeOfEnquiryAtDepartureReferenceNumber = Some(referenceNumber)
                          }
                        }

                        if (qName == CustomsOfficeOfDeparture || qName == CustomsOfficeOfDestinationActual) {
                          withinCustomsOfficeElements = false
                        }

                      }

                      override def startPrefixMapping(prefix: String, uri: String): Unit =
                        startPrefix = prefix

                    }
                  )

                  val rootNodeCheck = if (!elementValue.equalsIgnoreCase(rootTag.split(":")(1))) {
                    Either.left(BusinessValidationError("Root node doesn't match with the messageType"))
                  } else {
                    Either.right()
                  }

                  val referenceNumberCheck = if (!referenceNumber.toUpperCase.startsWith("GB") && !referenceNumber.toUpperCase.startsWith("XI")) {
                    Either.left(BusinessValidationError(s"Did not recognise office:$currentParentElement"))
                  } else {
                    Either.right(())
                  }

                  (rootNodeCheck, referenceNumberCheck) match {
                    case (Right(_), Right(_)) => Right(())
                    case (Left(error), _)     => Left(error)
                    case (_, Left(error))     => Left(error)
                  }

              }.toEither.leftMap {
                thr =>
                  ValidationError.Unexpected(Some(thr))
              }.flatten
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
