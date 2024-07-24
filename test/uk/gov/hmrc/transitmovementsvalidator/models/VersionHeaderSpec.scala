/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsvalidator.models

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VersionHeaderSpec extends AnyWordSpec with Matchers {

  "apply" should {
    "return Transitional if the headerValue is 'transitional'" in {
      VersionHeader.apply("transitional".some) shouldBe Transitional
    }

    "return Transitional regardless of casing/whitespace if the headerValue is ' Transitional '" in {
      VersionHeader.apply(" traNSitional ".some) shouldBe Transitional
    }

    "return Transitional if no APIVersion header has been provided" in {
      VersionHeader.apply(None) shouldBe Transitional
    }

    "return Final if the headerValue is 'final'" in {
      VersionHeader.apply("final".some) shouldBe Final
    }

    "return Final regardless of casing/whitespace if headerValue is ' Final '" in {
      VersionHeader.apply(" Final ".some) shouldBe Final
    }

    "return InvalidVersionHeader if the header is not final/transitional" in {
      VersionHeader.apply("Invalid".some) shouldBe InvalidVersionHeader("invalid")
    }
  }
}
