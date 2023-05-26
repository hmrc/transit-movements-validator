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
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import akka.util.Timeout
import cats.data.NonEmptyList
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.base.TestSourceProvider
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ErrorCode.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.FailedToParse

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Try

class JsonValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures with TestSourceProvider {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>
  lazy val validCode: String = "IE015"

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate" - {
    "when valid CC013C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc013c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE013", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC014C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC015C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC170C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc170c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE170", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC007C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC044C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC141C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc141c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE141", source)

      whenReady(result.value) {
        r =>
          r.isRight mustBe true
      }
    }

    "when no valid message type is provided, return UnknownMessageType" in {
      val source      = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-valid.json"))
      val invalidCode = "dummy"
      val sut         = new JsonValidationServiceImpl
      val result      = sut.validate(invalidCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.UnknownMessageType(invalidCode)
      }
    }

    "when valid message type provided but with schema invalid json, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")).isInstanceOf[ValidationError.JsonFailedValidation]
      }
    }

    "when an invalid CC014C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC014C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC007C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

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

    "when an invalid CC141C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc141c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE141", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:PreparationDateAndTimeContentType",
                  "$.n1:CC141C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with schema invalid date in the limitDate field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc013c-invalid-date.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE013", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:LimitDateContentType",
                  "$.n1:CC013C.TransitOperation.limitDate: does not match the date pattern - date provided is invalid."
                ),
                Nil
              )
            )
          )
      }
    }

    "when invalid json is provided, returns FailedToParse" in {
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, singleUseStringSource("{'ABC':}"))

      whenReady(result.value) {
        e =>
          e.left.get mustBe a[FailedToParse]
      }
    }

    "when invalid json is provided with extra braces, returns an exception" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/invalid-with-extra-braces.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result.value) {
        e =>
          e mustBe Left(FailedToParse("""Unexpected close marker '}': expected ']' (for root starting at [line: 1, column: 0])
              | at [line: 73, column: 2]""".stripMargin))
      }
    }

    "when an error occurs when parsing Json, ensure the source isn't included in the string" in {
      val sut    = new JsonValidationServiceImpl
      val source = Source.single(ByteString("{ nope }"))
      // This show throw a specific error
      Try(new ObjectMapper().readTree(source.runWith(StreamConverters.asInputStream(5.seconds)))) match {
        case Failure(x: JsonParseException) =>
          sut.stripSource(x.getMessage) mustBe
            """Unexpected character ('n' (code 110)): was expecting double-quote to start field name
              | at [line: 1, column: 4]""".stripMargin
        case _ => fail("A JsonParseException was not thrown")
      }
    }

    "when an invalid CC007C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC007CType/required",
                  "$.n1:CC007C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC007C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC007C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc013c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE013", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC013CType/required",
                  "$.n1:CC013C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC013C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc013c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE013", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC013C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC014C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

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
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

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
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE015", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC015C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC044C provided with invalid schema, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

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
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

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
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc170c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE170", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:CC170CType/required",
                  "$.n1:CC170C.messageSender: is missing but it is required"
                ),
                Nil
              )
            )
          )
      }
    }

    "when an invalid CC170C provided with invalid value in messageSender field, return error" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc170c-invalid-message-sender.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE170", source)

      whenReady(result.value) {
        r =>
          r mustBe Left(
            ValidationError.JsonFailedValidation(
              NonEmptyList(
                JsonSchemaValidationError(
                  "#/definitions/n1:MessageSenderContentType/pattern",
                  "$.n1:CC170C.messageSender: does not match the regex pattern ^([\\w\\D]{1,35})$"
                ),
                Nil
              )
            )
          )
      }
    }

    "when message type and root node doesn't match, return BusinessValidationError" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-rootNodeMismatch.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.businessRuleValidation("IE015", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.BusinessValidationError(
            "Root node doesn't match with the messageType"
          )
      }
    }

    "when referenceNumber node doesn't start with GB or XI for Arrival, return BusinessValidationError" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-reference-arrival.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.businessRuleValidation("IE007", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Invalid reference number: GZ123456")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference number: GZ123456"
          )
      }
    }

    "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid-reference-departure.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.businessRuleValidation("IE015", source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Invalid reference number: GV123456")) mustBe ValidationError.BusinessValidationError(
            "Invalid reference number: GV123456"
          )
      }
    }

  }
}
