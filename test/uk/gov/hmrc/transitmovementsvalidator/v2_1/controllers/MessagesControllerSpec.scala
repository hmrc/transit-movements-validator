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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.controllers

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import cats.data.EitherT
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.CONTENT_TYPE
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.status
import play.api.test.StubControllerComponentsFactory
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.v2_1.services.XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.v2_1.utils.NonEmptyListFormat
import uk.gov.hmrc.transitmovementsvalidator.v2_1.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.v2_1.config.AppConfig

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
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
    with NonEmptyListFormat
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  implicit val timeout: Timeout                           = Timeout(5.seconds)
  implicit val materializer: Materializer                 = Materializer(TestActorSystem.system)
  implicit val temporaryFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator

  val mockJsonValidationService: JsonValidationService         = mock[JsonValidationService]
  val mockXmlValidationService: XmlValidationService           = mock[XmlValidationService]
  val mockBusinessValidationService: BusinessValidationService = mock[BusinessValidationService]
  val mockConfig: AppConfig                                    = mock[AppConfig]
  when(mockConfig.validateRequestTypesOnly).thenReturn(true)

  val finalVersionHeaderValue = "final"

  override def beforeEach(): Unit = {
    reset(mockJsonValidationService)
    reset(mockXmlValidationService)
    reset(mockBusinessValidationService)
    super.beforeEach()
  }
  val validCode: MessageType = MessageType.DeclarationData
  val invalidCode            = "dummy"

  "On validate XML" - {

    lazy val validXml: NodeSeq = <test></test>

    "on a valid XML file, with the application/xml content type, return No Content" in forAll(Gen.oneOf(MessageType.requestValues)) {
      messageType =>
        when(mockXmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockBusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

        val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request =
          FakeRequest(
            "POST",
            s"/messages/${messageType.code}/validate/",
            FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
            source
          )
        val result = messagesController.validate(messageType.code)(request)

        status(result) mustBe NO_CONTENT
    }

    "on a valid XML file, but no valid message type, return BadRequest with an error message" in {
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$invalidCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
        source
      )
      val result = messagesController.validate(invalidCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"Unknown Message Type provided: $invalidCode is not recognised",
        "code"    -> "NOT_FOUND"
      )
      status(result) mustBe NOT_FOUND
    }

    "on a valid XML file for a response type where response types are not enabled, return BadRequest with an error message" in forAll(
      Gen.oneOf(MessageType.responseValues)
    ) {
      responseType =>
        val messagesController =
          new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

        val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request =
          FakeRequest(
            "POST",
            s"/messages/${responseType.code}/validate/",
            FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
            source
          )
        val result = messagesController.validate(responseType.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "message" -> s"Unknown Message Type provided: ${responseType.code} is not recognised",
          "code"    -> "NOT_FOUND"
        )
        status(result) mustBe NOT_FOUND
    }

    "on a valid XML file for a request or response type where response types are enabled, return No Content" in forAll(Gen.oneOf(MessageType.values)) {
      messageType =>
        when(mockXmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockBusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val config = mock[AppConfig]
        when(config.validateRequestTypesOnly).thenReturn(false)

        val messagesController =
          new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, config)

        val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request =
          FakeRequest(
            "POST",
            s"/messages/${messageType.code}/validate/",
            FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
            source
          )
        val result = messagesController.validate(messageType.code)(request)

        status(result) mustBe NO_CONTENT
    }

    "on an invalid XML file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, Unit](ValidationError.XmlFailedValidation(errorList))
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on an exception being thrown during xml schema validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error)))
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "root node doesn't match messageType in XML file, return BadRequest with an error message" in {
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ =>
            (EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      status(result) mustBe OK

      contentAsJson(result) mustBe
        Json.obj(
          "message" -> Json.toJson("Root node doesn't match with the messageType"),
          "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
        )
    }

    "on an exception being thrown during xml business validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")

      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error))), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }

  "On validate JSON" - {

    lazy val validJson: String   = "{}"
    lazy val invalidJson: String = "{"

    "on a valid JSON file, with the application/json content type, return No Content" in {
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a valid JSON file, but no valid message type, return BadRequest with an error message" in {
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$invalidCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(invalidCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"Unknown Message Type provided: $invalidCode is not recognised",
        "code"    -> "NOT_FOUND"
      )
      status(result) mustBe NOT_FOUND
    }

    "on an invalid JSON file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(
        JsonSchemaValidationError("IE015C:LRN", "'123456' exceeds maximum length of 4."),
        List(JsonSchemaValidationError("IE015C:MessageSender", "MessageSender element not in schema"))
      )

      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.JsonFailedValidation(errorList))
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(invalidJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on receiving an invalid content type, must return Unsupported Media Type" in {
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.TEXT)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type text/plain is not supported.")
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "with no content type header, must return Unsupported Media Type" in {
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq.empty), source)
      val result  = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type must be specified.")
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "on an exception being thrown during json validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )
      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "on an JsonParseException being thrown during json validation, must return Bad Request" in {
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ =>
            EitherT.leftT[Future, ValidationError](
              ValidationError.FailedToParse(
                "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
              )
            )
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj(
        "code" -> "BAD_REQUEST",
        "message" -> "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
      )
      status(result) mustBe BAD_REQUEST
    }

    "root node doesn't match messageType in json, return BadRequest with an error message" in {

      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ =>
            (EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      status(result) mustBe OK

      contentAsJson(result) mustBe
        Json.obj(
          "message" -> Json.toJson("Root node doesn't match with the messageType"),
          "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
        )
    }

    "on an exception being thrown during json business validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockBusinessValidationService.businessValidationFlow(eqTo(validCode), eqTo(MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => (EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error))), Flow[ByteString])
        )

      val messagesController =
        new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockBusinessValidationService, mockConfig)

      val source = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest(
        "POST",
        s"/messages/$validCode/validate/",
        FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)),
        source
      )
      val result = messagesController.validate(validCode.code)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }
}
