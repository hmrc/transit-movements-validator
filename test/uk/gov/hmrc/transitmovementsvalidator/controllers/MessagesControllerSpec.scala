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

package uk.gov.hmrc.transitmovementsvalidator.controllers

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import cats.data.EitherT
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.CONTENT_TYPE
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.status
import play.api.test.StubControllerComponentsFactory
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ObjectStoreError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.SchemaValidationPresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.response.ValidationResponse
import uk.gov.hmrc.transitmovementsvalidator.services.JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.services.ObjectStoreService
import uk.gov.hmrc.transitmovementsvalidator.services.XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.utils.NonEmptyListFormat

import java.nio.charset.StandardCharsets
import java.util.UUID.randomUUID
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
    with ScalaFutures {

  implicit val timeout: Timeout                           = Timeout(5.seconds)
  implicit val materializer: Materializer                 = Materializer(TestActorSystem.system)
  implicit val temporaryFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator

  lazy val filePath = Path
    .Directory(s"common-transit-convention-traders/movements/12345678")
    .file(randomUUID.toString)
    .asUri

  val mockJsonValidationService: JsonValidationService                 = mock[JsonValidationService]
  val mockXmlValidationService: XmlValidationService                   = mock[XmlValidationService]
  val mockObjectStoreService: ObjectStoreService                       = mock[ObjectStoreService]
  val mockObjectStoreURIHeaderExtractor: ObjectStoreURIHeaderExtractor = mock[ObjectStoreURIHeaderExtractor]

  override def beforeEach(): Unit = {
    reset(mockJsonValidationService)
    reset(mockXmlValidationService)
    super.beforeEach()
  }
  val validCode      = "IE015"
  val invalidCode    = "dummy"
  val objectStoreUri = "https://objectstore/file"

  "On validate XML" - {

    lazy val validXml: NodeSeq = <test></test>

    "on a valid XML file, with the application/xml content type, return No Content" in {
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockXmlValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a valid XML file, but no valid message type, return BadRequest with an error message" in {
      when(mockXmlValidationService.validate(eqTo(invalidCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.UnknownMessageType("dummy"))
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$invalidCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(invalidCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"Unknown Message Type provided: $invalidCode is not recognised",
        "code"    -> "NOT_FOUND"
      )
      status(result) mustBe NOT_FOUND
    }

    "on an invalid XML file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.XmlFailedValidation(errorList))
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on an exception being thrown during xml schema validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "root node doesn't match messageType in XML file, return BadRequest with an error message" in {
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockXmlValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType"))
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.XML)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe
        Json.obj(
          "message" -> Json.toJson("Root node doesn't match with the messageType"),
          "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
        )
    }

    "on an exception being thrown during xml business validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")

      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockXmlValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
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
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockJsonValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a valid JSON file, but no valid message type, return BadRequest with an error message" in {
      when(mockJsonValidationService.validate(eqTo(invalidCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.UnknownMessageType("dummy"))
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$invalidCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(invalidCode)(request)

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

      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.JsonFailedValidation(errorList))
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(invalidJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

    "on receiving an invalid content type, must return Unsupported Media Type" in {
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.TEXT)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type text/plain is not supported.")
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "on an exception being thrown during json validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
        )
      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "on an JsonParseException being thrown during json validation, must return Bad Request" in {
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ =>
            EitherT.leftT[Future, ValidationError](
              ValidationError.FailedToParse(
                "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
              )
            )
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "code"    -> "BAD_REQUEST",
        "message" -> "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
      )
      status(result) mustBe BAD_REQUEST
    }

    "root node doesn't match messageType in json, return BadRequest with an error message" in {

      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockJsonValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType"))
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe
        Json.obj(
          "message" -> Json.toJson("Root node doesn't match with the messageType"),
          "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
        )
    }

    "on an exception being thrown during json business validation, must return Internal Server Error" in {
      val error = new IllegalStateException("Unable to extract schema")
      when(mockJsonValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )
      when(mockJsonValidationService.businessRuleValidation(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
        )

      val sut     = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
      val request = FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(Seq(CONTENT_TYPE -> MimeTypes.JSON)), source)
      val result  = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }

  "ResponseCreator implicit class" - {
    import MessagesController.ResponseCreator

    "A Right Unit (success case) becomes a Right None" in {
      whenReady(EitherT.rightT[Future, PresentationError](()).toValidationResponse.value) {
        _ mustBe Right(None)
      }
    }

    "A Left SchemaValidationPresentationError (failed to validate - returns a success) becomes a Right Some of validation errors" - {

      "for XML validation errors" in {
        val error                                           = NonEmptyList.one(XmlSchemaValidationError(1, 1, "error"))
        val input: EitherT[Future, PresentationError, Unit] = EitherT.leftT[Future, Unit](SchemaValidationPresentationError(error))
        whenReady(input.toValidationResponse.value) {
          _ mustBe Right(Some(ValidationResponse(error)))
        }
      }

      "for Json validation errors" in {
        val error                                           = NonEmptyList.one(JsonSchemaValidationError("path", "error"))
        val input: EitherT[Future, PresentationError, Unit] = EitherT.leftT[Future, Unit](SchemaValidationPresentationError(error))
        whenReady(input.toValidationResponse.value) {
          _ mustBe Right(Some(ValidationResponse(error)))
        }
      }
    }

    "A Left non-schema presentation error does not get changed" in {
      val error = PresentationError.notFoundError("error")
      val input = EitherT.leftT[Future, Unit](error)
      whenReady(input.toValidationResponse.value) {
        _ mustBe Left(error)
      }
    }
  }

  "On validate Object Store resource" - {

    lazy val validSource = Source.single(ByteString(<test>test xml</test>.mkString, StandardCharsets.UTF_8))

    "on a valid file being streamed from object store with X-Object-Store-Uri present and no content type, return No Content" in {
      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.rightT[Future, ValidationError](())
        )

      when(mockObjectStoreURIHeaderExtractor.extractObjectStoreURI(any[Headers])).thenReturn(EitherT.rightT(ObjectStoreResourceLocation(filePath)))

      when(mockObjectStoreService.getContents(any[String].asInstanceOf[ObjectStoreResourceLocation])(any[ExecutionContext], any[HeaderCarrier]))
        .thenAnswer(
          _ => EitherT.rightT[Future, Source[ByteString, NotUsed]](validSource)
        )

      val request =
        FakeRequest(
          "POST",
          s"/messages/$validCode/validate/",
          FakeHeaders(Seq("X-Object-Store-Uri" -> ObjectStoreResourceLocation(filePath).value)),
          Source.empty
        )
      val sut    = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val result = sut.validate(validCode)(request)

      status(result) mustBe NO_CONTENT
    }

    "on a file being streamed from object store with neither X-Object-Store-Uri nor content type present, return Bad Request with an error message" in {
      val sut = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val request =
        FakeRequest("POST", s"/messages/$validCode/validate/", FakeHeaders(), Source.empty)
      val result = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj("message" -> "Missing X-Object-Store-Uri header value", "code" -> "BAD_REQUEST")
      status(result) mustBe BAD_REQUEST

    }

    "on object store unable to find the file located at X-Object-Store-Uri, return Bad Request with an error message" in {

      when(mockObjectStoreURIHeaderExtractor.extractObjectStoreURI(any[Headers])).thenReturn(EitherT.rightT(ObjectStoreResourceLocation(filePath)))

      when(mockObjectStoreService.getContents(any[String].asInstanceOf[ObjectStoreResourceLocation])(any[ExecutionContext], any[HeaderCarrier]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ObjectStoreError](ObjectStoreError.FileNotFound(objectStoreUri))
        )

      val request =
        FakeRequest(
          "POST",
          s"/messages/$validCode/validate/",
          FakeHeaders(Seq("X-Object-Store-Uri" -> ObjectStoreResourceLocation(filePath).value)),
          Source.empty
        )

      val sut    = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val result = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "code"    -> "BAD_REQUEST",
        "message" -> s"File not found at location: $objectStoreUri"
      )
      status(result) mustBe BAD_REQUEST
    }

    "on object store throwing an unexpected exception, return an Internal Server Error" in {

      val error = new IllegalStateException("Object Store problem")

      when(mockObjectStoreURIHeaderExtractor.extractObjectStoreURI(any[Headers])).thenReturn(EitherT.rightT(ObjectStoreResourceLocation(filePath)))

      when(mockObjectStoreService.getContents(any[String].asInstanceOf[ObjectStoreResourceLocation])(any[ExecutionContext], any[HeaderCarrier]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ObjectStoreError](ObjectStoreError.UnexpectedError(Some(error)))
        )

      val request =
        FakeRequest(
          "POST",
          s"/messages/$validCode/validate/",
          FakeHeaders(Seq("X-Object-Store-Uri" -> ObjectStoreResourceLocation(filePath).value)),
          Source.empty
        )

      val sut    = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val result = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> "Internal server error",
        "code"    -> "INTERNAL_SERVER_ERROR"
      )
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "on streaming an invalid XML file, return Ok with a list of errors" in {
      val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

      when(mockObjectStoreURIHeaderExtractor.extractObjectStoreURI(any[Headers])).thenReturn(EitherT.rightT(ObjectStoreResourceLocation(filePath)))

      when(mockObjectStoreService.getContents(any[String].asInstanceOf[ObjectStoreResourceLocation])(any[ExecutionContext], any[HeaderCarrier]))
        .thenAnswer(
          _ => EitherT.rightT[Future, Source[ByteString, NotUsed]](validSource)
        )

      when(mockXmlValidationService.validate(eqTo(validCode), any[Source[ByteString, _]])(any[Materializer], any[ExecutionContext]))
        .thenAnswer(
          _ => EitherT.leftT[Future, ValidationError](ValidationError.XmlFailedValidation(errorList))
        )

      val sut = new MessagesController(stubControllerComponents(), mockXmlValidationService, mockJsonValidationService, mockObjectStoreService)
      val request =
        FakeRequest(
          "POST",
          s"/messages/$validCode/validate/",
          FakeHeaders(Seq("X-Object-Store-Uri" -> ObjectStoreResourceLocation(filePath).value)),
          Source.empty
        )
      val result = sut.validate(validCode)(request)

      contentAsJson(result) mustBe Json.obj(
        "validationErrors" -> Json.toJson(errorList)
      )
      status(result) mustBe OK
    }

  }

}
