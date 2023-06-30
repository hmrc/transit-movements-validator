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

import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.NodeSeq

class BusinessValidationServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with ScalaCheckDrivenPropertyChecks with TestActorSystem {

  implicit val timeout: PatienceConfig = PatienceConfig(2.seconds, 2.seconds)

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/data"

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

  "Json" - {

    "when we don't validate the message recipient" - {

      lazy val appConfig = mock[AppConfig]
      when(appConfig.enableBusinessValidationMessageRecipient).thenReturn(false)

      "when message type doesn't exist, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-missingNode.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(ValidationError.MissingElementError(Seq("n1:CC007C", "messageType"))) => succeed
          case x =>
            fail(s"Expected a Left of MissingElementError, got $x")
        }
      }

      "when message type and root node doesn't match, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-rootNodeMismatch.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(ValidationError.BusinessValidationError("Root node doesn't match with the messageType")) => succeed
          case x =>
            fail(s"Expected a Left of root node error but got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Arrival, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-reference-arrival.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDestinationActual must be a customs office located in the United Kingdom (GZ123456 was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid-reference-departure.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (GV123456 was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when message is business rule valid, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-valid.json"))

        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

    }

    "when we do validate the message recipient" - {

      lazy val appConfig = mock[AppConfig]
      when(appConfig.enableBusinessValidationMessageRecipient).thenReturn(true)

      "when messageRecipient is invalid" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-recipient.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient must be either NTA.GB or NTA.XI (nope was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when the office country does not match the messageRecipient country (XI office for GB recipient)" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-ref-office.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient country must match the country of the CustomsOfficeOfDestinationActual"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid-reference-departure.json"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (GV123456 was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when message is business rule valid for GB, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid.json"))

        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when message is business rule valid for XI, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid-xi.json"))

        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

    }
  }

  "XML" - {

    "when we don't validate the message recipient" - {

      lazy val appConfig = mock[AppConfig]
      when(appConfig.enableBusinessValidationMessageRecipient).thenReturn(false)

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = Source.single(ByteString(invalidDepartureReferenceXml.mkString, StandardCharsets.UTF_8))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (GV1T34FR was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get expected message/result (got $x)")
        }
      }

      "when message is business rule valid, return a Right" in {
        val source         = FileIO.fromPath(Paths.get(testDataPath + "/cc015c-valid.xml"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when message type and root node doesn't match, return BusinessValidationError" in {
        val source         = Source.single(ByteString(rootNodeMismatchXml.mkString, StandardCharsets.UTF_8))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(ValidationError.BusinessValidationError("Root node doesn't match with the messageType")) => succeed
          case _                                                                                             => fail("Expected a Left but got a Right")
        }
      }

      "when message type is there too much, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(testDataPath + "/cc007c-tooManyNodes.xml"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(ValidationError.TooManyElementsError(Seq("CC007C", "messageType"))) => succeed
          case x                                                                        => fail(s"Expected a Left of TooManyElements, got $x")
        }
      }
    }

    "when we do validate the message recipient" - {

      lazy val appConfig = mock[AppConfig]
      when(appConfig.enableBusinessValidationMessageRecipient).thenReturn(true)

      "when messageRecipient is invalid" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-recipient.xml"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient must be either NTA.GB or NTA.XI (token was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when the office country does not match the messageRecipient country (XI office for GB recipient)" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-ref-office.xml"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient country must match the country of the CustomsOfficeOfDestinationActual"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid-reference-departure.xml"))
        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (XB3KMA8M was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when message is business rule valid for GB, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid.xml"))

        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when message is business rule valid for XI, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid-xi.xml"))

        val sut            = new BusinessValidationServiceImpl(appConfig)
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

    }
  }

}
