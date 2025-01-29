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

package uk.gov.hmrc.transitmovementsvalidator.routing

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.Action
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.config.Constants
import uk.gov.hmrc.transitmovementsvalidator.controllers.{MessagesController => TransitionalMessagesController}
import uk.gov.hmrc.transitmovementsvalidator.services.BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.v2_1.controllers.{MessagesController => FinalMessagesController}
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.{BusinessValidationService => FinalBusinessValidationService}
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.{JsonValidationService => FinalJsonValidationService}
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.{XmlValidationService => FinalXmlValidationService}

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VersionedRoutingControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  "validate" should {
    "call the transitional controller when APIVersion non 'final' value has been sent" in new Setup {
      val request = FakeRequest("POST", s"/messages/someCode/validate/", FakeHeaders(Seq(Constants.APIVersionHeaderKey -> "anything")), source)
      val result  = controller.validate("someCode")(request)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "transitional"
    }

    "call the transitional controller when APIVersion is not defined in the headers" in new Setup {
      val request = FakeRequest("POST", s"/messages/someCode/validate/", FakeHeaders(Seq.empty), source)
      val result  = controller.validate("someCode")(request)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "transitional"
    }

    "call the final controller when APIVersion 'final' has been sent" in new Setup {
      val request =
        FakeRequest("POST", s"/messages/someCode/validate/", FakeHeaders(Seq(Constants.APIVersionHeaderKey -> Constants.APIVersionFinalHeaderValue)), source)
      val result = controller.validate("someCode")(request)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "final"
    }
  }

  trait Setup {

    implicit val materializer: Materializer = Materializer(TestActorSystem.system)

    val source: Source[ByteString, NotUsed] = Source.single(ByteString("someValidSource", StandardCharsets.UTF_8))

    val mockXmlValidationService: XmlValidationService                     = mock[XmlValidationService]
    val mockJsonValidationService: JsonValidationService                   = mock[JsonValidationService]
    val mockBusinessValidationService: BusinessValidationService           = mock[BusinessValidationService]
    val mockConfig: AppConfig                                              = mock[AppConfig]
    val mockFileCreater: TemporaryFileCreator                              = mock[TemporaryFileCreator]
    val mockFinalXmlValidationService: FinalXmlValidationService           = mock[FinalXmlValidationService]
    val mockFinalJsonValidationService: FinalJsonValidationService         = mock[FinalJsonValidationService]
    val mockFinalBusinessValidationService: FinalBusinessValidationService = mock[FinalBusinessValidationService]
    val mockFinalConfig: AppConfig                                         = mock[AppConfig]
    val mockFinalFileCreater: TemporaryFileCreator                         = mock[TemporaryFileCreator]

    val mockTransitionalMesssagesController: TransitionalMessagesController = new TransitionalMessagesController(
      stubControllerComponents(),
      mockXmlValidationService,
      mockJsonValidationService,
      mockBusinessValidationService,
      mockConfig
    )(implicitly, mockFileCreater, implicitly) {

      override def validate(messageType: String): Action[Source[ByteString, ?]] = Action.async(streamFromMemory) {
        _ =>
          Future.successful(Ok("transitional"))
      }
    }

    val mockFinalMessagesController: FinalMessagesController =
      new FinalMessagesController(
        stubControllerComponents(),
        mockFinalXmlValidationService,
        mockFinalJsonValidationService,
        mockFinalBusinessValidationService,
        mockConfig
      )(
        implicitly,
        mockFileCreater,
        implicitly
      ) {

        override def validate(messageType: String): Action[Source[ByteString, ?]] = Action.async(streamFromMemory) {
          _ =>
            Future.successful(Ok("final"))
        }
      }

    val controller = new VersionedRoutingController(stubControllerComponents(), mockTransitionalMesssagesController, mockFinalMessagesController)
  }
}
