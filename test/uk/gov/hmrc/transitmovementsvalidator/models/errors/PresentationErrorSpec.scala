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

package uk.gov.hmrc.transitmovementsvalidator.models.errors

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class PresentationErrorSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "Test Json is as expected" - {
    def testStandard(function: String => PresentationError, message: String, code: String) = {
      val sut    = function(message)
      val result = Json.toJson(sut)(PresentationError.presentationErrorWrites)

      result mustBe Json.obj("message" -> message, "code" -> code)
    }

    "for UnsupportedMediaType" in testStandard(PresentationError.unsupportedMediaTypeError, "unsupported media type", "UNSUPPORTED_MEDIA_TYPE")

    "for BadRequest" in testStandard(PresentationError.badRequestError, "bad request", "BAD_REQUEST")

    "for NotFound" in testStandard(PresentationError.notFoundError, "not found", "NOT_FOUND")

    Seq(Some(new IllegalStateException("message")), None).foreach {
      exception =>
        val textFragment = exception
          .map(
            _ => "contains"
          )
          .getOrElse("does not contain")
        s"for an unexpected error that $textFragment a Throwable" in {
          // Given this exception
          val exception = new IllegalStateException("message")

          // when we create a error for this
          val sut: PresentationError = InternalServiceError(cause = Some(exception))

          // and when we turn it to Json
          val json = Json.toJson(sut)

          // then we should get an expected output
          json mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        }
    }
  }

}
