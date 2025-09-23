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

package uk.gov.hmrc.transitmovementsvalidator.v3_0.services

import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.transitmovementsvalidator.models.APIVersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType.ArrivalNotification
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.JsonFailedValidation
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.itbase.StreamTestHelpers
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.itbase.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.itbase.TestObjects
import uk.gov.hmrc.transitmovementsvalidator.v3_0.services.itbase.TestObjects.CC007C

import scala.concurrent.ExecutionContext.Implicits.global

class V3ValidatorServiceIntegrationSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with TestActorSystem
    with StreamTestHelpers {
  val jsonValidationService = new V3JsonValidationServiceImpl()
  val xmlValidationService  = new V3XmlValidationServiceImpl()

  val apiVersion = APIVersionHeader.V3_0

  "Json validation" - {

    "validating CC007C valid JSON returns right" in {
      val result = jsonValidationService.validate(ArrivalNotification(apiVersion), createStream(CC007C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.ArrivalNotification(apiVersion), createStream(TestObjects.CC007C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[JsonFailedValidation]
      }
    }

    "validating CC013C valid JSON returns right" in {
      val result = jsonValidationService.validate(MessageType.DeclarationAmendment(apiVersion), createStream(TestObjects.CC013C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.DeclarationAmendment(apiVersion), createStream(TestObjects.CC013C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC014C valid JSON returns right" in {
      val result = jsonValidationService.validate(MessageType.DeclarationInvalidation(apiVersion), createStream(TestObjects.CC014C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.DeclarationInvalidation(apiVersion), createStream(TestObjects.CC014C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC015C valid JSON returns right" in {
      val result = jsonValidationService.validate(MessageType.DeclarationData(apiVersion), createStream(TestObjects.CC015C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.DeclarationData(apiVersion), createStream(TestObjects.CC015C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC044C valid JSON returns right" in {
      val result = jsonValidationService.validate(MessageType.UnloadingRemarks(apiVersion), createStream(TestObjects.CC044C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.UnloadingRemarks(apiVersion), createStream(TestObjects.CC044C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }

    "validating CC170C valid JSON returns right" in {
      val result = jsonValidationService.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), createStream(TestObjects.CC170C.jsonValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid JSON returns JsonFailedValidation error" in {
      val result = jsonValidationService.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), createStream(TestObjects.CC170C.jsonInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.JsonFailedValidation]
      }
    }
  }

  "Xml validation" - {

    "validating CC007C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.ArrivalNotification(apiVersion), createStream(TestObjects.CC007C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC007C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.ArrivalNotification(apiVersion), createStream(TestObjects.CC007C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC013C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.DeclarationAmendment(apiVersion), createStream(TestObjects.CC013C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC013C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.DeclarationAmendment(apiVersion), createStream(TestObjects.CC013C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC014C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.DeclarationInvalidation(apiVersion), createStream(TestObjects.CC014C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC014C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.DeclarationInvalidation(apiVersion), createStream(TestObjects.CC014C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC015C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.DeclarationData(apiVersion), createStream(TestObjects.CC015C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC015C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.DeclarationData(apiVersion), createStream(TestObjects.CC015C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC044C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.UnloadingRemarks(apiVersion), createStream(TestObjects.CC044C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC044C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.UnloadingRemarks(apiVersion), createStream(TestObjects.CC044C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }

    "validating CC170C valid XML returns right" in {
      val result = xmlValidationService.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), createStream(TestObjects.CC170C.xmlValid))
      whenReady(result.value) {
        either =>
          either.isRight mustBe true
      }
    }

    "validating CC170C invalid XML returns XmlFailedValidation error" in {
      val result = xmlValidationService.validate(MessageType.PresentationNotificationForPreLodgedDec(apiVersion), createStream(TestObjects.CC170C.xmlInvalid))
      whenReady(result.value) {
        either => either.left.getOrElse(()) mustBe a[ValidationError.XmlFailedValidation]
      }
    }
  }
}
