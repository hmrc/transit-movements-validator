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

package uk.gov.hmrc.transitmovementsvalidator.services.jsonformats

import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DateTimeFormatSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  "matches" - {
    "is true for a datetime that can be parsed" in {
      DateTimeFormat.matches("2022-10-25T00:01:02") mustBe true
    }

    "is true for a datetime with milliseconds can be parsed" in {
      DateTimeFormat.matches("2022-10-25T00:01:02.000") mustBe true
    }

    "is false for just a date" in {
      DateTimeFormat.matches("2022-14-25") mustBe false
    }

    "is false for a datetime that cannot be parsed due to an incorrect date" in {
      DateTimeFormat.matches("2022-14-25T00:01:02") mustBe false
    }

    "is false for a datetime that cannot be parsed due to an incorrect time" in {
      DateTimeFormat.matches("2022-12-25T34:01:02") mustBe false
    }

    "is false for a datetime that cannot be parsed due a random string" in forAll(Gen.alphaNumStr) {
      string =>
        DateTimeFormat.matches(string) mustBe false
    }
  }

}
