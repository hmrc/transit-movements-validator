/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType.*

class MessageTypeSpec extends AnyFreeSpec with Matchers with MockitoSugar with OptionValues with ScalaCheckDrivenPropertyChecks {
  "MessageType must contain for APIVersion 2.1" - {
    val apiVersion: APIVersionHeader = APIVersionHeader.V2_1
    "UnloadingRemarks" in {
      MessageType.values(apiVersion) must contain(UnloadingRemarks(apiVersion))
      UnloadingRemarks(apiVersion).code mustEqual "IE044"
      UnloadingRemarks(apiVersion).rootNode mustEqual "CC044C"
      MessageType.arrivalRequestValues(apiVersion) must contain(UnloadingRemarks(apiVersion))
    }

    "ArrivalNotification" in {
      MessageType.values(apiVersion) must contain(ArrivalNotification(apiVersion))
      ArrivalNotification(apiVersion).code mustEqual "IE007"
      ArrivalNotification(apiVersion).rootNode mustEqual "CC007C"
      MessageType.arrivalRequestValues(apiVersion) must contain(ArrivalNotification(apiVersion))
    }

    "DeclarationAmendment" in {
      MessageType.values(apiVersion) must contain(DeclarationAmendment(apiVersion))
      DeclarationAmendment(apiVersion).code mustEqual "IE013"
      DeclarationAmendment(apiVersion).rootNode mustEqual "CC013C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationAmendment(apiVersion))
    }

    "DeclarationInvalidation" in {
      MessageType.values(apiVersion) must contain(DeclarationInvalidation(apiVersion))
      DeclarationInvalidation(apiVersion).code mustEqual "IE014"
      DeclarationInvalidation(apiVersion).rootNode mustEqual "CC014C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationInvalidation(apiVersion))
    }

    "DeclarationData" in {
      MessageType.values(apiVersion) must contain(DeclarationData(apiVersion))
      DeclarationData(apiVersion).code mustEqual "IE015"
      DeclarationData(apiVersion).rootNode mustEqual "CC015C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationData(apiVersion))
    }

    "PresentationNotificationForPreLodgedDec" in {
      MessageType.values(apiVersion) must contain(PresentationNotificationForPreLodgedDec(apiVersion))
      PresentationNotificationForPreLodgedDec(apiVersion).code mustEqual "IE170"
      PresentationNotificationForPreLodgedDec(apiVersion).rootNode mustEqual "CC170C"
      MessageType.departureRequestValues(apiVersion) must contain(PresentationNotificationForPreLodgedDec(apiVersion))
    }
  }
  "MessageType must contain for APIVersion 3.0" - {
    val apiVersion: APIVersionHeader = APIVersionHeader.V3_0
    "UnloadingRemarks" in {
      MessageType.values(apiVersion) must contain(UnloadingRemarks(apiVersion))
      UnloadingRemarks(apiVersion).code mustEqual "IE044"
      UnloadingRemarks(apiVersion).rootNode mustEqual "CC044C"
      MessageType.arrivalRequestValues(apiVersion) must contain(UnloadingRemarks(apiVersion))
    }

    "ArrivalNotification" in {
      MessageType.values(apiVersion) must contain(ArrivalNotification(apiVersion))
      ArrivalNotification(apiVersion).code mustEqual "IE007"
      ArrivalNotification(apiVersion).rootNode mustEqual "CC007C"
      MessageType.arrivalRequestValues(apiVersion) must contain(ArrivalNotification(apiVersion))
    }

    "DeclarationAmendment" in {
      MessageType.values(apiVersion) must contain(DeclarationAmendment(apiVersion))
      DeclarationAmendment(apiVersion).code mustEqual "IE013"
      DeclarationAmendment(apiVersion).rootNode mustEqual "CC013C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationAmendment(apiVersion))
    }

    "DeclarationInvalidation" in {
      MessageType.values(apiVersion) must contain(DeclarationInvalidation(apiVersion))
      DeclarationInvalidation(apiVersion).code mustEqual "IE014"
      DeclarationInvalidation(apiVersion).rootNode mustEqual "CC014C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationInvalidation(apiVersion))
    }

    "DeclarationData" in {
      MessageType.values(apiVersion) must contain(DeclarationData(apiVersion))
      DeclarationData(apiVersion).code mustEqual "IE015"
      DeclarationData(apiVersion).rootNode mustEqual "CC015C"
      MessageType.departureRequestValues(apiVersion) must contain(DeclarationData(apiVersion))
    }

    "PresentationNotificationForPreLodgedDec" in {
      MessageType.values(apiVersion) must contain(PresentationNotificationForPreLodgedDec(apiVersion))
      PresentationNotificationForPreLodgedDec(apiVersion).code mustEqual "IE170"
      PresentationNotificationForPreLodgedDec(apiVersion).rootNode mustEqual "CC170C"
      MessageType.departureRequestValues(apiVersion) must contain(PresentationNotificationForPreLodgedDec(apiVersion))
    }
  }
  "find for APIVersion 2.1" - {
    val apiVersion = APIVersionHeader.V2_1
    "must return None when junk is provided" in forAll(Gen.stringOfN(6, Gen.alphaNumChar)) {
      code =>
        MessageType.find(code, true, apiVersion) must not be defined
    }

    "must return the correct message type when a correct code is provided for request types" in forAll(Gen.oneOf(MessageType.requestValues(apiVersion))) {
      messageType =>
        MessageType.find(messageType.code, true, apiVersion) mustBe Some(messageType)
    }

    "must return None when a correct code is provided for response types, when requestOnly is true" in forAll(
      Gen.oneOf(MessageType.responseValues(apiVersion))
    ) {
      messageType =>
        MessageType.find(messageType.code, true, apiVersion) mustBe None
    }

    "must return the correct message type when a correct code is provided for all types" in forAll(Gen.oneOf(MessageType.values(apiVersion))) {
      messageType =>
        MessageType.find(messageType.code, false, apiVersion) mustBe Some(messageType)
    }
  }
  "find for APIVersion 3.0" - {
    val apiVersion = APIVersionHeader.V3_0
    "must return None when junk is provided" in forAll(Gen.stringOfN(6, Gen.alphaNumChar)) {
      code =>
        MessageType.find(code, true, apiVersion) must not be defined
    }

    "must return the correct message type when a correct code is provided for request types" in forAll(Gen.oneOf(MessageType.requestValues(apiVersion))) {
      messageType =>
        MessageType.find(messageType.code, true, apiVersion) mustBe Some(messageType)
    }

    "must return None when a correct code is provided for response types, when requestOnly is true" in forAll(
      Gen.oneOf(MessageType.responseValues(apiVersion))
    ) {
      messageType =>
        MessageType.find(messageType.code, true, apiVersion) mustBe None
    }

    "must return the correct message type when a correct code is provided for all types" in forAll(Gen.oneOf(MessageType.values(apiVersion))) {
      messageType =>
        MessageType.find(messageType.code, false, apiVersion) mustBe Some(messageType)
    }
  }
}
