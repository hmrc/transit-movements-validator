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

package uk.gov.hmrc.transitmovementsvalidator.controllers

import cats.data.NonEmptyList
import cats.syntax.all._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsvalidator.models.errors.InternalServiceError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationPresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ErrorTranslatorSpec extends AnyFreeSpec with Matchers with OptionValues with ScalaFutures with MockitoSugar with ScalaCheckDrivenPropertyChecks {
  object Harness extends ErrorTranslator

  import Harness._

  "ErrorConverter#asPresentation" - {
    "for a success returns the same right" in {
      val input = Right[ValidationError, Unit](()).toEitherT[Future]
      whenReady(input.asPresentation.value) {
        _ mustBe Right(())
      }
    }

    "for an error returns a left with the appropriate presentation error" in {
      val error = new IllegalStateException()
      val input = Left[ValidationError, Unit](ValidationError.Unexpected(Some(error))).toEitherT[Future]
      whenReady(input.asPresentation.value) {
        _ mustBe Left(InternalServiceError(cause = Some(error)))
      }
    }
  }

  "Validation Error" - {

    import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError._

    "an Unexpected Error with no exception returns an internal service error with no exception" in {
      val input  = Unexpected(None)
      val output = InternalServiceError()

      validationErrorConverter.convert(input) mustBe output
    }

    "an Unexpected Error with an exception returns an internal service error with an exception" in {
      val exception = new IllegalStateException()
      val input     = Unexpected(Some(exception))
      val output    = InternalServiceError(cause = Some(exception))

      validationErrorConverter.convert(input) mustBe output
    }

    "UnknownMessageType converts to a NotFound" in forAll(Gen.alphaStr) {
      t =>
        val input  = UnknownMessageType(t)
        val output = PresentationError.notFoundError(s"Unknown Message Type provided: $t is not recognised")

        validationErrorConverter.convert(input) mustBe output
    }

    "FailedToParse becomes a BadRequest" in {
      val input  = FailedToParse("message")
      val output = PresentationError.badRequestError("message")

      validationErrorConverter.convert(input) mustBe output
    }

    "FailedValidationError to SchemaValidationError" - {
      "for XML errors" in {
        val input  = XmlFailedValidation(NonEmptyList.one(XmlSchemaValidationError(1, 1, "message")))
        val output = SchemaValidationPresentationError(input.errors)

        validationErrorConverter.convert(input) mustBe output
      }

      "for Json errors" in {
        val input  = JsonFailedValidation(NonEmptyList.one(JsonSchemaValidationError("path", "message")))
        val output = SchemaValidationPresentationError(input.errors)

        validationErrorConverter.convert(input) mustBe output
      }
    }

    "BusinessValidationError becomes a BadRequest" in {
      val input  = BusinessValidationError("Root node doesn't match with the messageType")
      val output = PresentationError.businessValidationError("Root node doesn't match with the messageType")

      validationErrorConverter.convert(input) mustBe output
    }
  }

}
