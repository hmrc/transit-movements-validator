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
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import cats.data.NonEmptyList
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

class XmlValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>

  lazy val rootNodeMismatchXml: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>OJ8tELE5IIgfuH2C3RepK5tFCVJo5fJ9</messageSender>
    <messageRecipient>OJ8tELE5IIgfuH2C3RepK5tFCVJo5fJ9</messageRecipient>
    <preparationDateAndTime>2022-12-20T10:34:40</preparationDateAndTime>
    <messageIdentification>XusMGrh</messageIdentification>
    <messageType>CC007C</messageType>
    </ncts:CC015C>

  lazy val invalidArrivalSingleReferenceXml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageType>CC007C</messageType>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>GZ123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
    </ncts:CC007C>

  lazy val invalidArrivalMultipleReferenceXml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageType>CC007C</messageType>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>JK123456</referenceNumber>
        <referenceNumber>GF123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
    </ncts:CC007C>

  lazy val invalidReferenceXml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC007C</messageType>
      <correlationIdentifier>token</correlationIdentifier>
      <TransitOperation>
        <MRN>27WF9X1FQ9RCKN0TM3</MRN>
        <arrivalNotificationDateAndTime>2022-07-02T03:11:04</arrivalNotificationDateAndTime>
        <simplifiedProcedure>1</simplifiedProcedure>
        <incidentFlag>1</incidentFlag>
      </TransitOperation>
      <Authorisation>
        <sequenceNumber>123</sequenceNumber>
        <type>3344</type>
        <referenceNumber>token</referenceNumber>
      </Authorisation>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>GZ123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
      <TraderAtDestination>
        <identificationNumber>ezv3Z</identificationNumber>
        <communicationLanguageAtDestination>sa</communicationLanguageAtDestination>
      </TraderAtDestination>
      <Consignment>
        <LocationOfGoods>
          <typeOfLocation>A</typeOfLocation>
          <qualifierOfIdentification>A</qualifierOfIdentification>
          <authorisationNumber>token</authorisationNumber>
          <additionalIdentifier>1234</additionalIdentifier>
          <UNLocode>token</UNLocode>
          <CustomsOffice>
            <referenceNumber>AB234567</referenceNumber>
          </CustomsOffice>

          <EconomicOperator>
            <identificationNumber>ezv3Z</identificationNumber>
          </EconomicOperator>
          <Address>
            <streetAndNumber>token</streetAndNumber>
            <postcode>token</postcode>
            <city>token</city>
            <country>GB</country>
          </Address>
          <PostcodeAddress>
            <houseNumber>token</houseNumber>
            <postcode>token</postcode>
            <country>SA</country>
          </PostcodeAddress>
          <ContactPerson>
            <name>token</name>
            <phoneNumber>token</phoneNumber>
            <eMailAddress>sandeep@gmail.com</eMailAddress>
          </ContactPerson>
        </LocationOfGoods>
        <Incident>
          <sequenceNumber>12345</sequenceNumber>
          <code>1</code>
          <text>token</text>
          <Endorsement>
            <date>2022-07-02</date>
            <authority>token</authority>
            <place>token</place>
            <country>GB</country>
          </Endorsement>
          <Location>
            <qualifierOfIdentification>A</qualifierOfIdentification>
            <UNLocode>token</UNLocode>
            <country>SA</country>

            <Address>
              <streetAndNumber>token</streetAndNumber>
              <postcode>token</postcode>
              <city>token</city>
            </Address>
          </Location>
          <TransportEquipment>
            <sequenceNumber>12345</sequenceNumber>
            <containerIdentificationNumber>ezv3Z</containerIdentificationNumber>
            <numberOfSeals>2345</numberOfSeals>
            <Seal>
              <sequenceNumber>12345</sequenceNumber>
              <identifier>token</identifier>
            </Seal>
            <GoodsReference>
              <sequenceNumber>12345</sequenceNumber>
              <declarationGoodsItemNumber>12</declarationGoodsItemNumber>
            </GoodsReference>
          </TransportEquipment>
          <Transhipment>
            <containerIndicator>0</containerIndicator>
            <TransportMeans>
              <typeOfIdentification>12</typeOfIdentification>
              <identificationNumber>ezv3Z</identificationNumber>
              <nationality>GB</nationality>
            </TransportMeans>
          </Transhipment>
        </Incident>
      </Consignment>
    </ncts:CC007C>

  lazy val invalidDepartureSingleReferenceXml: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageType>CC015C</messageType>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GZ123456</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC015C>

  lazy val invalidDepartureMultipleReferenceXml: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageType>CC015C</messageType>
      <CustomsOfficeOfDeparture>
        <referenceNumber>JK123456</referenceNumber>
        <referenceNumber>GF123456</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC015C>

  lazy val validCode: String = "IE015"

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate XML" - {

    "when valid XML IE013 is provided for the given message type, return a Right" in {
      val ie13File = scala.io.Source.fromFile(testDataPath + "/cc013c-valid.xml")
      try {
        val source = Source.single(ByteString(ie13File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE013", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie13File.close
    }

    "when valid XML IE014 is provided for the given message type, return a Right" in {
      val ie14File = scala.io.Source.fromFile(testDataPath + "/cc014c-valid.xml")
      try {
        val source = Source.single(ByteString(ie14File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE014", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie14File.close
    }

    "when valid XML IE015 is provided for the given message type, return a Right" in {
      val ie15File = scala.io.Source.fromFile(testDataPath + "/cc015c-valid.xml")
      try {
        val source = Source.single(ByteString(ie15File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(validCode, source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie15File.close
    }

    "when valid XML IE170 is provided for the given message type, return a Right" in {
      val ie170File = scala.io.Source.fromFile(testDataPath + "/cc170c-valid.xml")
      try {
        val source = Source.single(ByteString(ie170File.mkString, StandardCharsets.UTF_8)) //exampleIE170XML.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE170", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie170File.close()
    }

    "when valid XML IE007 is provided for the given message type, return a Right" in {
      val ie007File = scala.io.Source.fromFile(testDataPath + "/cc007c-valid.xml")
      try {
        val source = Source.single(ByteString(ie007File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE007", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie007File.close()
    }

    "when valid XML IE044 is provided for the given message type, return a Right" in {
      val ie044File = scala.io.Source.fromFile(testDataPath + "/cc044c-valid.xml")
      try {
        val source = Source.single(ByteString(ie044File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE044", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie044File.close()
    }

    "when valid XML IE141 is provided for the given message type, return a Right" in {
      val ie141File = scala.io.Source.fromFile(testDataPath + "/cc141c-valid.xml")
      try {
        val source = Source.single(ByteString(ie141File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE141", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie141File.close()
    }

    "when no valid message type is provided, return UnknownMessageTypeValidationError" in {
      val source      = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val invalidCode = "dummy"
      val sut         = new XmlValidationServiceImpl
      val result      = sut.validate(invalidCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.UnknownMessageType(invalidCode)
      }
    }

    "when valid message type provided but with unexpected xml, return errors" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")).isInstanceOf[ValidationError.XmlFailedValidation]
      }
    }

    "when invalid XML IE007 is provided, return XmlSchemaValidationError" in {
      val ie7invalidFile = scala.io.Source.fromFile(testDataPath + "/cc007c-invalid.xml")
      try {
        val source = Source.single(ByteString(ie7invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE007", source)

        whenReady(result.value) {
          r =>
            r mustBe Left(
              ValidationError.XmlFailedValidation(
                NonEmptyList(
                  XmlSchemaValidationError(
                    2,
                    23,
                    "cvc-complex-type.2.4.a: Invalid content was found starting with element 'messageRecipient'. One of '{messageSender}' is expected."
                  ),
                  Nil
                )
              )
            )
        }
      } finally ie7invalidFile.close()
    }

    "when invalid XML IE007 is provided with invalid value for messageSender, return XmlSchemaValidationError" in {
      val ie7invalidFile = scala.io.Source.fromFile(testDataPath + "/cc007c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie7invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE007", source)

        whenReady(result.value) {
          r =>
            r mustBe Left(
              ValidationError.XmlFailedValidation(
                NonEmptyList(
                  XmlSchemaValidationError(
                    2,
                    36,
                    "cvc-pattern-valid: Value '' is not facet-valid with respect to pattern '.{1,35}' for type 'MessageSenderContentType'."
                  ),
                  List(XmlSchemaValidationError(2, 36, "cvc-type.3.1.3: The value '' of element 'messageSender' is not valid."))
                )
              )
            )
        }
      } finally ie7invalidFile.close()
    }

    "when invalid XML IE013 is provided, return left" in {
      val ie13invalidFile = scala.io.Source.fromFile(testDataPath + "/cc013c-invalid.xml")
      try {
        val source = Source.single(ByteString(ie13invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE013", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie13invalidFile.close()
    }

    "when invalid XML IE013 is provided with invalid value for messageSender, return left" in {
      val ie13invalidFile = scala.io.Source.fromFile(testDataPath + "/cc013c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie13invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE013", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie13invalidFile.close()
    }

    "when invalid XML IE014 is provided, return left" in {
      val ie14invalidFile = scala.io.Source.fromFile(testDataPath + "/cc014c-invalid.xml")
      try {
        val source = Source.single(ByteString(ie14invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE014", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie14invalidFile.close()
    }

    "when invalid XML IE014 is provided with invalid value for messageSender, return left" in {
      val ie14invalidFile = scala.io.Source.fromFile(testDataPath + "/cc014c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie14invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE014", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie14invalidFile.close()
    }

    "when invalid XML IE015 is provided with invalid value for messageSender, return left" in {
      val ie15invalidFile = scala.io.Source.fromFile(testDataPath + "/cc015c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie15invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE015", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie15invalidFile.close()
    }

    "when invalid XML IE044 is provided, return left" in {
      val ie44invalidFile = scala.io.Source.fromFile(testDataPath + "/cc044c-invalid.xml")
      try {
        val source = Source.single(ByteString(ie44invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE044", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie44invalidFile.close()
    }

    "when invalid XML IE044 is provided with invalid value for messageSender, return left" in {
      val ie44invalidFile = scala.io.Source.fromFile(testDataPath + "/cc044c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie44invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE044", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie44invalidFile.close()
    }

    "when invalid XML IE170 is provided, return left" in {
      val ie170invalidFile = scala.io.Source.fromFile(testDataPath + "/cc170c-invalid.xml")
      try {
        val source = Source.single(ByteString(ie170invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE170", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie170invalidFile.close()
    }

    "when invalid XML IE170 is provided with invalid value for messageSender, return left" in {
      val ie170invalidFile = scala.io.Source.fromFile(testDataPath + "/cc170c-invalid-message-sender.xml")
      try {
        val source = Source.single(ByteString(ie170invalidFile.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(messageType = "IE170", source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie170invalidFile.close()
    }

    "when message type and root node doesn't match, return BusinessValidationError" in {
      val source = Source.single(ByteString(rootNodeMismatchXml.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.businessRuleValidation("IE015", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Root node doesn't match with the messageType"
          )
      }
    }

    "when referenceNumber doesn't start with GB or XI for Arrival, return BusinessValidationError, given single referenceNumber" in {
      val source = Source.single(ByteString(invalidArrivalSingleReferenceXml.mkString, StandardCharsets.UTF_8))
      val sut = new XmlValidationServiceImpl
      val result = sut.businessRuleValidation("IE007", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference numbers: GZ123456"
          )
      }
    }

    "when referenceNumber doesn't start with GB or XI for Arrival, return BusinessValidationError, given multiple referenceNumbers" in {
      val source = Source.single(ByteString(invalidArrivalMultipleReferenceXml.mkString, StandardCharsets.UTF_8))
      val sut = new XmlValidationServiceImpl
      val result = sut.businessRuleValidation("IE007", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference numbers: JK123456, GF123456"
          )
      }
    }

    "when referenceNumber doesn't start with GB or XI for Departure, return BusinessValidationError, given single referenceNumber" in {
      val source = Source.single(ByteString(invalidDepartureSingleReferenceXml.mkString, StandardCharsets.UTF_8))
      val sut = new XmlValidationServiceImpl
      val result = sut.businessRuleValidation("IE015", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference numbers: GZ123456"
          )
      }
    }

    "when referenceNumber doesn't start with GB or XI for Departure, return BusinessValidationError, given multiple referenceNumbers" in {
      val source = Source.single(ByteString(invalidDepartureMultipleReferenceXml.mkString, StandardCharsets.UTF_8))
      val sut = new XmlValidationServiceImpl
      val result = sut.businessRuleValidation("IE015", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference numbers: JK123456, GF123456"
          )
      }
    }





  }
}
