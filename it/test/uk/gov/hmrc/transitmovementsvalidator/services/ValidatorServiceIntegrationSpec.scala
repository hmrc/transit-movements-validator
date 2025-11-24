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

import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.JsonFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.services.itbase.StreamTestHelpers
import uk.gov.hmrc.transitmovementsvalidator.services.itbase.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.services.itbase.TestObjectsV2_1
import uk.gov.hmrc.transitmovementsvalidator.services.itbase.TestObjectsV3_0
import uk.gov.hmrc.transitmovementsvalidator.services.itbase.TestObjectsV2_1.CC007C
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.JsonValidationService as V2JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.XmlValidationService as V2XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.JsonValidationService as V3JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.XmlValidationService as V3XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.models.MessageType as V2MessageType
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageType as V3MessageType

import scala.concurrent.ExecutionContext.Implicits.global

class ValidatorServiceIntegrationSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience with TestActorSystem with StreamTestHelpers {
  val v2JsonValidationService = new V2JsonValidationService()
  val v2XmlValidationService  = new V2XmlValidationService()

  val v3JsonValidationService = new V3JsonValidationService()
  val v3XmlValidationService  = new V3XmlValidationService()

  "Json validation for ApiVersion 2.1 " - {
    "validating CC007C valid JSON returns right" in {
      val result = v2JsonValidationService.validate(V2MessageType.ArrivalNotification, createStream(CC007C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(V2MessageType.ArrivalNotification, createStream(TestObjectsV2_1.CC007C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[JsonFailedValidation]
      }
    }

    "validating CC013C valid JSON returns right" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationAmendment, createStream(TestObjectsV2_1.CC013C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationAmendment, createStream(TestObjectsV2_1.CC013C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC014C valid JSON returns right" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationInvalidation, createStream(TestObjectsV2_1.CC014C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationInvalidation, createStream(TestObjectsV2_1.CC014C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC015C valid JSON returns right" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationData, createStream(TestObjectsV2_1.CC015C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(V2MessageType.DeclarationData, createStream(TestObjectsV2_1.CC015C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC044C valid JSON returns right" in {
      val result = v2JsonValidationService.validate(V2MessageType.UnloadingRemarks, createStream(TestObjectsV2_1.CC044C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(V2MessageType.UnloadingRemarks, createStream(TestObjectsV2_1.CC044C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC170C valid JSON returns right" in {
      val result =
        v2JsonValidationService.validate(V2MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV2_1.CC170C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid JSON returns JsonFailedValidation error" in {
      val result = v2JsonValidationService.validate(
        V2MessageType.PresentationNotificationForPreLodgedDec,
        createStream(TestObjectsV2_1.CC170C.jsonInvalid)
      )
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }
  }
  "Xml validation for ApiVersion 2.1" - {
    "validating CC007C valid XML returns right" in {
      val result = v2XmlValidationService.validate(V2MessageType.ArrivalNotification, createStream(TestObjectsV2_1.CC007C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid XML returns XmlFailedValidation error" in {
      val result = v2XmlValidationService.validate(V2MessageType.ArrivalNotification, createStream(TestObjectsV2_1.CC007C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC013C valid XML returns right" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationAmendment, createStream(TestObjectsV2_1.CC013C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid XML returns XmlFailedValidation error" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationAmendment, createStream(TestObjectsV2_1.CC013C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC014C valid XML returns right" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationInvalidation, createStream(TestObjectsV2_1.CC014C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid XML returns XmlFailedValidation error" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationInvalidation, createStream(TestObjectsV2_1.CC014C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC015C valid XML returns right" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationData, createStream(TestObjectsV2_1.CC015C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid XML returns XmlFailedValidation error" in {
      val result = v2XmlValidationService.validate(V2MessageType.DeclarationData, createStream(TestObjectsV2_1.CC015C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC044C valid XML returns right" in {
      val result = v2XmlValidationService.validate(V2MessageType.UnloadingRemarks, createStream(TestObjectsV2_1.CC044C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid XML returns XmlFailedValidation error" in {
      val result = v2XmlValidationService.validate(V2MessageType.UnloadingRemarks, createStream(TestObjectsV2_1.CC044C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC170C valid XML returns right" in {
      val result =
        v2XmlValidationService.validate(V2MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV2_1.CC170C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid XML returns XmlFailedValidation error" in {
      val result =
        v2XmlValidationService.validate(V2MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV2_1.CC170C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }
  }
  "Json validation for ApiVersion 3.0 " - {
    "validating CC007C valid JSON returns right" in {
      val result = v3JsonValidationService.validate(V3MessageType.ArrivalNotification, createStream(TestObjectsV3_0.CC007C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(V3MessageType.ArrivalNotification, createStream(TestObjectsV3_0.CC007C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[JsonFailedValidation]
      }
    }

    "validating CC013C valid JSON returns right" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationAmendment, createStream(TestObjectsV3_0.CC013C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationAmendment, createStream(TestObjectsV3_0.CC013C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC014C valid JSON returns right" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationInvalidation, createStream(TestObjectsV3_0.CC014C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationInvalidation, createStream(TestObjectsV3_0.CC014C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC015C valid JSON returns right" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationData, createStream(TestObjectsV3_0.CC015C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(V3MessageType.DeclarationData, createStream(TestObjectsV3_0.CC015C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC044C valid JSON returns right" in {
      val result = v3JsonValidationService.validate(V3MessageType.UnloadingRemarks, createStream(TestObjectsV3_0.CC044C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(V3MessageType.UnloadingRemarks, createStream(TestObjectsV3_0.CC044C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC170C valid JSON returns right" in {
      val result =
        v3JsonValidationService.validate(V3MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV3_0.CC170C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid JSON returns JsonFailedValidation error" in {
      val result = v3JsonValidationService.validate(
        V3MessageType.PresentationNotificationForPreLodgedDec,
        createStream(TestObjectsV2_1.CC170C.jsonInvalid)
      )
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }
  }
  "Xml validation for ApiVersion 3.0" - {
    "validating CC007C valid XML returns right" in {
      val result = v3XmlValidationService.validate(V3MessageType.ArrivalNotification, createStream(TestObjectsV3_0.CC007C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid XML returns XmlFailedValidation error" in {
      val result = v3XmlValidationService.validate(V3MessageType.ArrivalNotification, createStream(TestObjectsV3_0.CC007C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC013C valid XML returns right" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationAmendment, createStream(TestObjectsV3_0.CC013C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid XML returns XmlFailedValidation error" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationAmendment, createStream(TestObjectsV3_0.CC013C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC014C valid XML returns right" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationInvalidation, createStream(TestObjectsV3_0.CC014C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid XML returns XmlFailedValidation error" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationInvalidation, createStream(TestObjectsV3_0.CC014C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC015C valid XML returns right" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationData, createStream(TestObjectsV3_0.CC015C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid XML returns XmlFailedValidation error" in {
      val result = v3XmlValidationService.validate(V3MessageType.DeclarationData, createStream(TestObjectsV3_0.CC015C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC044C valid XML returns right" in {
      val result = v3XmlValidationService.validate(V3MessageType.UnloadingRemarks, createStream(TestObjectsV3_0.CC044C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid XML returns XmlFailedValidation error" in {
      val result = v3XmlValidationService.validate(V3MessageType.UnloadingRemarks, createStream(TestObjectsV3_0.CC044C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC170C valid XML returns right" in {
      val result =
        v3XmlValidationService.validate(V3MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV3_0.CC170C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid XML returns XmlFailedValidation error" in {
      val result =
        v3XmlValidationService.validate(V3MessageType.PresentationNotificationForPreLodgedDec, createStream(TestObjectsV3_0.CC170C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }
  }
}
