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

package uk.gov.hmrc.transitmovementsvalidator.controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.status
import play.api.test.StubControllerComponentsFactory
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

class MessagesControllerSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with StubControllerComponentsFactory
  with TestActorSystem {

  implicit val timeout      = Timeout(5.seconds)

  lazy val validXml: NodeSeq = <test></test>

  "On validate" - {
    "on a valid XML file, return OK and a true value for the successful validation" in {
      val sut     = new MessagesController(stubControllerComponents())
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", "/messages/cc015b/validate/", FakeHeaders(), source)
      val result  = sut.validate("cc015b")(request)

      contentAsJson(result) mustBe Json.obj("success" -> true)
      status(result) mustBe OK
    }
  }

}
