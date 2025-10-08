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

package uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services

import org.apache.pekko.stream.scaladsl.FileIO
import org.apache.pekko.stream.scaladsl.Sink
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageType

import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessValidationServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with ScalaCheckDrivenPropertyChecks with TestActorSystem {

  implicit val timeout: PatienceConfig = PatienceConfig(2.seconds, 2.seconds)

  lazy val v3TestDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/versioned/v3_0/data"

  def createConfig(lrnValidationEnabled: Boolean = true, lrnValidationRegex: String = "^.{1,35}$"): AppConfig = {
    val config = mock[AppConfig]
    when(config.validateLrnEnabled).thenReturn(lrnValidationEnabled)
    when(config.validateLrnRegex).thenReturn(lrnValidationRegex.r)
    config
  }

  "Json for ApiVersion 3.0" - {
    "when we validate the message recipient" - {

      "when messageRecipient is invalid" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-recipient.json"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient must be either NTA.GB or NTA.XI (nope was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when the office country does not match the messageRecipient country (XI office for GB recipient)" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-ref-office.json"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient country must match the country of the CustomsOfficeOfDestinationActual"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-reference-departure.json"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (GV123456 was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when message is business rule valid for GB, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-valid.json"))

        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when message is business rule valid for XI, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-valid-xi.json"))

        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

    }

    "when we validate the LRN" - {
      "when the LRN is 'invalid' and the LRN validation is disabled, no errors are returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-lrn.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when the LRN is 'invalid' and the LRN validation is enabled, an error is returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-lrn.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationRegex = "^[a-zA-Z]{1,35}$"))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(
            ValidationError.BusinessValidationError(
              "LRN must match the regex ^[a-zA-Z]{1,35}$, but '{{LRN}}' was provided"
            )
          )
        }
      }

      "when the LRN is 'valid' and the LRN validation is enabled, no errors are returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-valid.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationRegex = "^[a-zA-Z]{1,35}$"))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }
    }

    "IE013" - {
      "when we have a valid IE013 using an LRN, Unit must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-valid.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when we have a valid IE013 using an MRN, Unit must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-valid-mrn.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when we have an invalid IE013 without a LRN or MRN, a validation error must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-no-mrn-lrn.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(ValidationError.BusinessValidationError("A LRN or MRN must be specified, neither were found (rule C0467)"))
        }
      }

      "when we have an invalid IE013 with both a LRN and a MRN, a validation error must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-both-mrn-and-lrn.json"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Json)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(ValidationError.BusinessValidationError("Only an LRN or MRN must be specified, both were found (rule C0467)"))
        }
      }
    }
  }

  "XML for ApiVersion 2.1" - {
    "when we validate the message recipient" - {
      "when messageRecipient is invalid" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-recipient.xml"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient must be either NTA.GB or NTA.XI (token was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when the office country does not match the messageRecipient country (XI office for GB recipient)" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-invalid-ref-office.xml"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The message recipient country must match the country of the CustomsOfficeOfDestinationActual"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when referenceNumber node doesn't start with GB or XI for Departure, return BusinessValidationError" in {
        val source         = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-reference-departure.xml"))
        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          case Left(
                ValidationError.BusinessValidationError(
                  "The customs office specified for CustomsOfficeOfDeparture must be a customs office located in the United Kingdom (XB3KMA8M was specified)"
                )
              ) =>
            succeed
          case x => fail(s"Did not get Left of office error, got $x")
        }
      }

      "when message is business rule valid for GB, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-valid.xml"))

        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when message is business rule valid for XI, return a Right" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc007c-valid-xi.xml"))

        val sut            = new BusinessValidationService(createConfig())
        val (preMat, flow) = sut.businessValidationFlow(MessageType.ArrivalNotification, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

    }

    "when we validate the LRN" - {
      "when the LRN is 'invalid' and the LRN validation is disabled, no errors are returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-lrn.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when the LRN is 'invalid' and the LRN validation is enabled, an error is returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-invalid-lrn.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationRegex = "^[a-zA-Z]{1,35}$"))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(
            ValidationError.BusinessValidationError(
              "LRN must match the regex ^[a-zA-Z]{1,35}$, but '{{LRN}}' was provided"
            )
          )
        }
      }

      "when the LRN is 'valid' and the LRN validation is enabled, no errors are returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc015c-valid.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationRegex = "^[a-zA-Z]{1,35}$"))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationData, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }
    }

    "IE013" - {
      "when we have a valid IE013 using an LRN, Unit must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-valid.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when we have a valid IE013 using an MRN, Unit must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-valid-mrn.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Right((): Unit)
        }
      }

      "when we have an invalid IE013 without an LRN or MRN, a validation error must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-no-mrn-lrn.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(ValidationError.BusinessValidationError("A LRN or MRN must be specified, neither were found (rule C0467)"))
        }
      }

      "when we have an invalid IE013 with both a LRN and a MRN, a validation error must be returned" in {
        val source = FileIO.fromPath(Paths.get(s"$v3TestDataPath/cc013c-invalid-both-mrn-and-lrn.xml"))

        val sut            = new BusinessValidationService(createConfig(lrnValidationEnabled = false))
        val (preMat, flow) = sut.businessValidationFlow(MessageType.DeclarationAmendment, MessageFormat.Xml)

        source.via(flow).runWith(Sink.ignore)

        whenReady(preMat.value) {
          _ mustBe Left(ValidationError.BusinessValidationError("Only an LRN or MRN must be specified, both were found (rule C0467)"))
        }
      }
    }
  }
}
