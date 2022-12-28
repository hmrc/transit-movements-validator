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

package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import cats.data.NonEmptyList
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

class XmlValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>
  lazy val validCode: String = "IE015"

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate XML" - {

    "when valid XML IE013 is provided for the given message type, return a Right" in {
      val ie13File = scala.io.Source.fromFile(testDataPath + "/cc013c-valid.xml")
      try {
        val source = Source.single(ByteString(ie13File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE013", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie13File.close
    }

    "when valid XML IE014 is provided for the given message type, return a Right" in {
      val ie14File = scala.io.Source.fromFile(testDataPath + "/cc014c-valid.xml")
      try {
        val source = Source.single(ByteString(ie14File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE014", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie14File.close
    }

    "when valid XML IE015 is provided for the given message type, return a Right" in {
      val ie15File = scala.io.Source.fromFile(testDataPath + "/cc015c-valid.xml")
      try {
        val source = Source.single(ByteString(ie15File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate(validCode, source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie15File.close
    }

    "when valid XML IE170 is provided for the given message type, return a Right" in {
      val ie170File = scala.io.Source.fromFile(testDataPath + "/cc170c-valid.xml")
      try {
        val source = Source.single(ByteString(ie170File.mkString, StandardCharsets.UTF_8)) //exampleIE170XML.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE170", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie170File.close()
    }

    "when valid XML IE007 is provided for the given message type, return a Right" in {
      val ie007File = scala.io.Source.fromFile(testDataPath + "/cc007c-valid.xml")
      try {
        val source = Source.single(ByteString(ie007File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE007", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie007File.close()
    }

    "when valid XML IE044 is provided for the given message type, return a Right" in {
      val ie044File = scala.io.Source.fromFile(testDataPath + "/cc044c-valid.xml")
      try {
        val source = Source.single(ByteString(ie044File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE044", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie044File.close()
    }

    "when valid XML IE141 is provided for the given message type, return a Right" in {
      val ie141File = scala.io.Source.fromFile(testDataPath + "/cc141c-valid.xml")
      try {
        val source = Source.single(ByteString(ie141File.mkString, StandardCharsets.UTF_8))
        val sut    = new XmlValidationServiceImpl
        val result = sut.validate("IE141", source)

        whenReady(result.value) {
          r =>
            r.isRight mustBe true
        }
      } finally ie141File.close()
    }

    "when no valid message type is provided, return UnknownMessageTypeValidationError" in {
      val source      = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val invalidCode = "dummy"
      val sut         = new XmlValidationServiceImpl
      val result      = sut.validate(invalidCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")) mustBe ValidationError.UnknownMessageType(invalidCode)
      }
    }

    "when valid message type provided but with unexpected xml, return errors" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result.value) {
        r =>
          r.left.getOrElse(fail("Expected a Left but got a Right")).isInstanceOf[ValidationError.XmlFailedValidation]
      }
    }
  }
}
