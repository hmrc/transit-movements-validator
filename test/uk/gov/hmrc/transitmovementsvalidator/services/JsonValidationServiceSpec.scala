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

import cats.data.NonEmptyList
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.FileIO
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.StreamConverters
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.base.TestSourceProvider
import uk.gov.hmrc.transitmovementsvalidator.models.APIVersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.FailedToParse

import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Try
import scala.xml.NodeSeq

class JsonValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures with TestSourceProvider {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>

  lazy val v2TestDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/v2_1/data"
  lazy val v3TestDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/v3_0/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate for ApiVersion 2.1" - {
    val apiVersion: APIVersionHeader = APIVersionHeader.V2_1
    "when valid CC013C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc013c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC014C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc014c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC015C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc015c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }

    }

    "when valid CC170C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc170c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC007C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc007c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC044C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc044c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid message type provided but with schema invalid json, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc015c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")).isInstanceOf[ValidationError.JsonFailedValidation]
      }
    }

    "when an invalid CC014C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc014c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC014C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                List()
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc007c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:PreparationDateAndTimeContentType",
                    "$.n1:CC007C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc044c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC044C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with schema invalid date in the limitDate field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc013c-invalid-date.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:LimitDateContentType",
                    "$.n1:CC013C.TransitOperation.limitDate: does not match the date pattern - date provided is invalid."
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when invalid json is provided, returns FailedToParse" in {
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), singleUseStringSource("{'ABC':}"), apiVersion)

      whenReady(result.value) {
        case Left(_: FailedToParse) => succeed
        case _                      => fail("Expected Left(FailedToPasrse)")
      }
    }

    "when invalid json is provided with extra braces, returns an exception" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/invalid-with-extra-braces.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        e =>
          e mustBe Left(
            FailedToParse(
              """Unexpected close marker '}': expected ']' (for root starting at [line: 1, column: 0])
              | at [line: 73, column: 2]""".stripMargin
            )
          )
      }
    }

    "when an error occurs when parsing Json, ensure the source isn't included in the string" in {
      val sut    = new JsonValidationService
      val source = Source.single(ByteString("{ nope }"))
      // This should throw a specific error
      Try(new ObjectMapper().readTree(source.runWith(StreamConverters.asInputStream(5.seconds)))) match {
        case Failure(x: JsonParseException) =>
          sut.stripSource(x.getMessage) mustBe
            """Unexpected character ('n' (code 110)): was expecting double-quote to start field name
              | at [line: 1, column: 4]""".stripMargin
        case _ => fail("A JsonParseException was not thrown")
      }
    }

    "when an invalid CC007C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc007c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC007CType/required", "$.n1:CC007C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc007c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC007C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc013c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC013CType/required", "$.n1:CC013C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc013c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC013C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC014C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc014c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC014CType/required",
                  "$.n1:CC014C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC014C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc014c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC014C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC015C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc015c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC015C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC015C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC015C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC015C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC015C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc044c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC044CType/required",
                  "$.n1:CC044C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc044c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC044C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC170C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc170c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC170C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC170CType/required", "$.n1:CC170C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC170C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v2TestDataPath/cc170c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC170C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC170C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

  }
  "On Validate for ApiVersion 3.0" - {
    val apiVersion: APIVersionHeader = APIVersionHeader.V3_0
    "when valid CC013C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC014C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc014c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC015C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }

    }

    "when valid CC170C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc170c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC007C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC044C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc044c-valid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid message type provided but with schema invalid json, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")).isInstanceOf[ValidationError.JsonFailedValidation]
      }
    }

    "when an invalid CC014C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc014c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC014C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                List()
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:PreparationDateAndTimeContentType",
                    "$.n1:CC007C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc044c-invalid-date-time.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC044C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with schema invalid date in the limitDate field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-date.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:LimitDateContentType",
                    "$.n1:CC013C.TransitOperation.limitDate: does not match the date pattern - date provided is invalid."
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when invalid json is provided, returns FailedToParse" in {
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), singleUseStringSource("{'ABC':}"), apiVersion)

      whenReady(result.value) {
        case Left(_: FailedToParse) => succeed
        case _                      => fail("Expected Left(FailedToPasrse)")
      }
    }

    "when invalid json is provided with extra braces, returns an exception" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/invalid-with-extra-braces.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        e =>
          e mustBe Left(
            FailedToParse(
              """Unexpected close marker '}': expected ']' (for root starting at [line: 1, column: 0])
              | at [line: 73, column: 2]""".stripMargin
            )
          )
      }
    }

    "when an error occurs when parsing Json, ensure the source isn't included in the string" in {
      val sut    = new JsonValidationService
      val source = Source.single(ByteString("{ nope }"))
      // This should throw a specific error
      Try(new ObjectMapper().readTree(source.runWith(StreamConverters.asInputStream(5.seconds)))) match {
        case Failure(x: JsonParseException) =>
          sut.stripSource(x.getMessage) mustBe
            """Unexpected character ('n' (code 110)): was expecting double-quote to start field name
              | at [line: 1, column: 4]""".stripMargin
        case _ => fail("A JsonParseException was not thrown")
      }
    }

    "when an invalid CC007C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC007CType/required", "$.n1:CC007C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.ArrivalNotification(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC007C.Consignment.Incident[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC007C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC013CType/required", "$.n1:CC013C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationAmendment(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC013C.Consignment.HouseConsignment[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.CountryOfRoutingOfConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC013C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].SupportingDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Guarantee[0].GuaranteeReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].Commodity.DangerousGoods[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfExitForTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.CustomsOfficeOfTransitDeclared[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalInformation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].PreviousDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Authorisation[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].ConsignmentItem[0].AdditionalSupplyChainActor[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.HouseConsignment[0].AdditionalReference[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.TransportDocument[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC013C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC014C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc014c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC014CType/required",
                  "$.n1:CC014C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC014C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc014c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationInvalidation(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC014C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC015C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.DeclarationData(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC015C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC015C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC015C.Consignment.HouseConsignment[0].ConsignmentItem[0].Packaging[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC015C.Guarantee[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:GoodsItemNumberContentType/type",
                    "$.n1:CC015C.Consignment.HouseConsignment[0].ConsignmentItem[0].goodsItemNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc044c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC044CType/required",
                  "$.n1:CC044C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc044c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.UnloadingRemarks(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC044C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC170C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc170c-invalid.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC170C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError("#/definitions/n1:CC170CType/required", "$.n1:CC170C.messageSender: is missing but it is required"),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

    "when an invalid CC170C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc170c-invalid-message-sender.json"))
      val sut    = new JsonValidationService
      val result = sut.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), source, apiVersion)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:SequenceNumberContentType/type",
                  "$.n1:CC170C.Consignment.HouseConsignment[0].DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                ),
                List(
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].Seal[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:MessageSenderContentType/pattern",
                    "$.n1:CC170C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.DepartureTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.ActiveBorderTransportMeans[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.HouseConsignment[0].sequenceNumber: string found, integer expected"
                  ),
                  JsonSchemaValidationError(
                    "#/definitions/n1:SequenceNumberContentType/type",
                    "$.n1:CC170C.Consignment.TransportEquipment[0].GoodsReference[0].sequenceNumber: string found, integer expected"
                  )
                )
              )
            )
          )
      }
    }

  }
}
