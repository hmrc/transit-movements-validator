/*
 * Copyright 2022 HM Revenue & Customs
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
import akka.util.Timeout
import cats.data.NonEmptyList
import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.base.TestSourceProvider
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

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

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC014C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC015C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC170C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc170c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE170", source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC007C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC044C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when valid CC141C JSON is provided for the given message type, return a Right" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc141c-valid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE141", source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when no valid message type is provided, return UnknownMessageTypeValidationError" in {
      val source      = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-valid.json"))
      val invalidCode = "dummy"
      val sut         = new JsonValidationServiceImpl
      val result      = sut.validate(invalidCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head mustBe ValidationError.fromUnrecognisedMessageType(invalidCode)
      }
    }

    "when valid message type provided but with schema invalid json, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc015c-invalid.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head.isInstanceOf[JsonSchemaValidationError]
      }
    }

    "when an invalid CC014C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc014c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE014", source)

      whenReady(result) {
        r =>
          r mustBe Left(
            NonEmptyList(
              JsonSchemaValidationError(
                "#/definitions/n1:PreparationDateAndTimeContentType",
                "$.n1:CC014C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
              ),
              Nil
            )
          )
      }
    }

    "when an invalid CC007C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc007c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE007", source)

      whenReady(result) {
        r =>
          r mustBe Left(
            NonEmptyList(
              JsonSchemaValidationError(
                "#/definitions/n1:PreparationDateAndTimeContentType",
                "$.n1:CC007C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
              ),
              Nil
            )
          )
      }
    }

    "when an invalid CC044C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc044c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE044", source)

      whenReady(result) {
        r =>
          r mustBe Left(
            NonEmptyList(
              JsonSchemaValidationError(
                "#/definitions/n1:PreparationDateAndTimeContentType",
                "$.n1:CC044C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
              ),
              Nil
            )
          )
      }
    }

    "when an invalid CC141C provided with schema invalid datetime in the preparationDateAndTime field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc141c-invalid-date-time.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE141", source)

      whenReady(result) {
        r =>
          r mustBe Left(
            NonEmptyList(
              JsonSchemaValidationError(
                "#/definitions/n1:PreparationDateAndTimeContentType",
                "$.n1:CC141C.preparationDateAndTime: does not match the date-time pattern - date or time provided is invalid."
              ),
              Nil
            )
          )
      }
    }

    "when an invalid CC013C provided with schema invalid date in the limitDate field, return errors" in {
      val source = FileIO.fromPath(Paths.get(s"$testDataPath/cc013c-invalid-date.json"))
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate("IE013", source)

      whenReady(result) {
        r =>
          r mustBe Left(
            NonEmptyList(
              JsonSchemaValidationError(
                "#/definitions/n1:LimitDateContentType",
                "$.n1:CC013C.TransitOperation.limitDate: does not match the date pattern - date provided is invalid."
              ),
              Nil
            )
          )
      }
    }

    "when invalid json is provided, returns an exception" in {
      val sut    = new JsonValidationServiceImpl
      val result = sut.validate(validCode, singleUseStringSource("{'ABC':}"))

      whenReady(result.failed) {
        e =>
          e mustBe a[JsonParseException]
      }
    }

  }
}
