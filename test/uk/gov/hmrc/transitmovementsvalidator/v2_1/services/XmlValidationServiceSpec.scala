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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.services

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import cats.data.NonEmptyList
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.v2_1.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors.XmlSchemaValidationError

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

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

  lazy val invalidArrivalReferenceXml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageType>CC007C</messageType>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>GZ123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
    </ncts:CC007C>

  lazy val invalidDepartureReferenceXml: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>cUusObWiVZhuaZFNr6KrXy8y</messageRecipient>
      <preparationDateAndTime>2022-10-23T15:19:29</preparationDateAndTime>
      <messageIdentification>P</messageIdentification>
      <messageType>CC015C</messageType>
      <TransitOperation>
        <LRN>3CnsTh79I7vtOW1</LRN>
        <declarationType>G8</declarationType>
        <additionalDeclarationType>p</additionalDeclarationType>
        <security>9</security>
        <reducedDatasetIndicator>1</reducedDatasetIndicator>
        <bindingItinerary>1</bindingItinerary>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GV1T34FR</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <HolderOfTheTransitProcedure>
        <identificationNumber>k</identificationNumber>
      </HolderOfTheTransitProcedure>
      <Guarantee>
        <sequenceNumber>64582</sequenceNumber>
        <guaranteeType>J</guaranteeType>
        <otherGuaranteeReference>4yFxS49</otherGuaranteeReference>
      </Guarantee>
      <Consignment>
        <grossMass>1854093104.078068</grossMass>
        <HouseConsignment>
          <sequenceNumber>64582</sequenceNumber>
          <grossMass>1854093104.078068</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>39767</goodsItemNumber>
            <declarationGoodsItemNumber>1861</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>OPDK4mBmHFyczZqwPjzU5wqgynvlbKtDxc64BAXycRKIOlWGT7YDJcpGNUtmgbs79eqBw2gHpBJ2CRFkOp6RbHh7ZZp0HBtwk8q0mdKZbSdebOLEWzMrVYziNHyHa95fw7iiQonwKfCw6KA0NQQEFaFwmg6D</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>64582</sequenceNumber>
              <typeOfPackages>V8</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC015C>

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/v2_1/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate XML" - {

    "when valid XML IE013 is provided for the given message type, return a Right" in {
      val ie13File = scala.io.Source.fromFile(testDataPath + "/cc013c-valid.xml")
      try {
        val source = Source.single(ByteString(ie13File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(MessageType.DeclarationAmendment, source)

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
        val result = sut.validate(MessageType.DeclarationInvalidation, source)

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
        val result = sut.validate(MessageType.DeclarationData, source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie15File.close
    }

    "when valid XML IE015 with additional namespaces is provided for the given message type, return a Right" in {
      val ie15File = scala.io.Source.fromFile(testDataPath + "/cc015c-valid-2.xml")
      try {
        val source = Source.single(ByteString(ie15File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(MessageType.DeclarationData, source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie15File.close
    }

    "when valid XML IE170 is provided for the given message type, return a Right" in {
      val ie170File = scala.io.Source.fromFile(testDataPath + "/cc170c-valid.xml")
      try {
        val source = Source.single(ByteString(ie170File.mkString, StandardCharsets.UTF_8)) // exampleIE170XML.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec, source)

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
        val result = sut.validate(MessageType.ArrivalNotification, source)

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
        val result = sut.validate(MessageType.UnloadingRemarks, source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie044File.close()
    }

    "when valid message type provided but with unexpected xml, return errors" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.validate(MessageType.DeclarationData, source)

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
        val result = sut.validate(messageType = MessageType.ArrivalNotification, source)

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
        val result = sut.validate(messageType = MessageType.ArrivalNotification, source)

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
        val result = sut.validate(messageType = MessageType.DeclarationAmendment, source)

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
        val result = sut.validate(messageType = MessageType.DeclarationAmendment, source)

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
        val result = sut.validate(messageType = MessageType.DeclarationInvalidation, source)

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
        val result = sut.validate(messageType = MessageType.DeclarationInvalidation, source)

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
        val result = sut.validate(messageType = MessageType.DeclarationData, source)

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
        val result = sut.validate(messageType = MessageType.UnloadingRemarks, source)

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
        val result = sut.validate(messageType = MessageType.UnloadingRemarks, source)

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
        val result = sut.validate(messageType = MessageType.PresentationNotificationForPreLodgedDec, source)

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
        val result = sut.validate(messageType = MessageType.PresentationNotificationForPreLodgedDec, source)

        whenReady(result.value) {
          r => r.isLeft mustBe true
        }
      } finally ie170invalidFile.close()
    }

  }
}
