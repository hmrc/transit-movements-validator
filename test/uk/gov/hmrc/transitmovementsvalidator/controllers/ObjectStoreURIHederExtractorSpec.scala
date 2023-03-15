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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Headers
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.transitmovementsvalidator.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError

class ObjectStoreURIHederExtractorSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  class ObjectStoreURIHeaderExtractorImpl extends ObjectStoreURIHeaderExtractor with BaseController {
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  val objectStoreURIExtractor = new ObjectStoreURIHeaderExtractorImpl

  "extractObjectStoreURI" - {

    "if object store uri header is not supplied, return BadRequestError" in {
      val noObjectStoreURIHeader = Headers("X-Message-Type" -> "IE015")

      val result = objectStoreURIExtractor.extractObjectStoreURI(noObjectStoreURIHeader)

      whenReady(result.value) {
        _ mustBe Left(PresentationError.badRequestError("Missing X-Object-Store-Uri header value"))
      }
    }

    "if object store uri header supplied is invalid, return BadRequestError" in {
      val invalidObjectStoreURIHeader = Headers("X-Object-Store-Uri" -> "invalid")

      val result = objectStoreURIExtractor.extractObjectStoreURI(invalidObjectStoreURIHeader)

      whenReady(result.value) {
        _ mustBe Left(
          PresentationError.badRequestError(s"X-Object-Store-Uri header value does not start with common-transit-convention-traders/ (got invalid)")
        )
      }
    }

    "if object store uri header supplied is valid, return Right" in {
      val filePath                  = "common-transit-convention-traders/movements/movementId/abc.xml"
      val objectStoreURI            = ObjectStoreResourceLocation(filePath).value
      val validObjectStoreURIHeader = Headers("X-Object-Store-Uri" -> objectStoreURI)

      val result = objectStoreURIExtractor.extractObjectStoreURI(validObjectStoreURIHeader)

      whenReady(result.value) {
        _ mustBe Right(ObjectStoreResourceLocation("movements/movementId/abc.xml"))
      }
    }

  }

}
