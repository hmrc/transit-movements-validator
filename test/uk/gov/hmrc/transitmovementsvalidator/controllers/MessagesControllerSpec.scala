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

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.json.Json
import play.api.test.Helpers.CONTENT_TYPE
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.status
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.StubControllerComponentsFactory
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.services.ValidationService
import uk.gov.hmrc.transitmovementsvalidator.utils.NonEmptyListFormat

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

class MessagesControllerSpec
    extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with StubControllerComponentsFactory
    with TestActorSystem
    with BeforeAndAfterEach
    with NonEmptyListFormat {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  // the execution context for testing
  import system.dispatcher

  val mockValidationService: ValidationService = mock[ValidationService]

  override def beforeEach(): Unit = {
    reset(mockValidationService)
    super.beforeEach()
  }
  val validCode   = "IE015"
  val invalidCode = "dummy"

  "On validate XML" - {

    lazy val validXml: NodeSeq = <test></test>

    "on a valid XML file, with the application/xml content type, return No Content" in {
      when(mockValidationService.validateXML(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(())))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a valid XML file, but no valid message type, return BadRequest with an error message" in {
      when(mockValidationService.validateXML(eqTo(invalidCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(NonEmptyList(ValidationError.fromUnrecognisedMessageType("dummy"), Nil))))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$invalidCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(invalidCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"Unknown Message Type provided: $invalidCode is not recognised",
        "code"    -> "BAD_REQUEST"
      )
      status(result) mustBe BAD_REQUEST
    }

    "on an invalid XML file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

      when(mockValidationService.validateXML(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(errorList)))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on an exception being thrown during xml validation, must return Internal Server Error" in {
      when(mockValidationService.validateXML(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.failed(new Exception("Unable to extract schema")))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "On validate JSON" - {

    lazy val validJson: String   = "{}"
    lazy val invalidJson: String = "{"

    "on a valid JSON file, with the application/json content type, return No Content" in {
      when(mockValidationService.validateJSON(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Right(())))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a valid JSON file, with no content type, must return Unsupported Media Type" in {
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type must be specified.")
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "on a valid JSON file, but no valid message type, return BadRequest with an error message" in {
      when(mockValidationService.validateJSON(eqTo(invalidCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(NonEmptyList(ValidationError.fromUnrecognisedMessageType("dummy"), Nil))))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$invalidCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(invalidCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"Unknown Message Type provided: $invalidCode is not recognised",
        "code"    -> "BAD_REQUEST"
      )
      status(result) mustBe BAD_REQUEST
    }

    "on an invalid JSON file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(JsonSchemaValidationError("LRN", "'123456' exceeds maximum length of 4."), Nil)

      when(mockValidationService.validateJSON(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.successful(Left(errorList)))

      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(invalidJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on receiving an invalid content type, must return Unsupported Media Type" in {
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.TEXT)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type text/plain is not supported.")
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "on an exception being thrown during json validation, must return Internal Server Error" in {
      when(mockValidationService.validateJSON(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenReturn(Future.failed(new Exception("Unable to extract schema")))
      val sut     = new MessagesController(stubControllerComponents(), mockValidationService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }

}
