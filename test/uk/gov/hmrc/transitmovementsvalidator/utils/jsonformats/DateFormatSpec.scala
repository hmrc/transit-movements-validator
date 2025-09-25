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

package uk.gov.hmrc.transitmovementsvalidator.utils.jsonformats

import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DateFormatSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  "matches" - {
    "is true for a date that can be parsed" in {
      DateFormat.matches("2022-10-25") mustBe true
    }

    "is false for a date that cannot be parsed" in {
      DateFormat.matches("2022-14-25") mustBe false
    }

    "is false for a datetime that cannot be parsed" in {
      DateFormat.matches("2022-14-25") mustBe false
    }

    "is false for a date that cannot be parsed due a random string" in forAll(Gen.alphaNumStr) {
      string =>
        DateFormat.matches(string) mustBe false
    }
  }

}
