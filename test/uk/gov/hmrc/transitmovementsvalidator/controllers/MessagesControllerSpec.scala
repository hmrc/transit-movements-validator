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

import cats.data.EitherT
import cats.data.NonEmptyList
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.*
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.test.Helpers.CONTENT_TYPE
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.status
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.StubControllerComponentsFactory
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.controllers.actions.ValidateAcceptRefiner
import uk.gov.hmrc.transitmovementsvalidator.models.errors.JsonSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.utils.NonEmptyListFormat
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.JsonValidationService as V2JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.XmlValidationService as V2XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.services.BusinessValidationService as V2BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.JsonValidationService as V3JsonValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.XmlValidationService as V3XmlValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.services.BusinessValidationService as V3BusinessValidationService
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.models.MessageType as V2MessageType
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageType as V3MessageType
import uk.gov.hmrc.transitmovementsvalidator.versioned.v2_1.models.MessageFormat as V2MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.versioned.v3_0.models.MessageFormat as V3MessageFormat

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
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
    with NonEmptyListFormat
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  implicit val timeout: Timeout                           = Timeout(5.seconds)
  implicit val materializer: Materializer                 = Materializer(TestActorSystem.system)
  implicit val temporaryFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator

  val mockV2JsonValidationService: V2JsonValidationService         = mock[V2JsonValidationService]
  val mockV2XmlValidationService: V2XmlValidationService           = mock[V2XmlValidationService]
  val mockV2BusinessValidationService: V2BusinessValidationService = mock[V2BusinessValidationService]

  val mockV3JsonValidationService: V3JsonValidationService         = mock[V3JsonValidationService]
  val mockV3XmlValidationService: V3XmlValidationService           = mock[V3XmlValidationService]
  val mockV3BusinessValidationService: V3BusinessValidationService = mock[V3BusinessValidationService]

  val mockConfig: AppConfig = mock[AppConfig]
  when(mockConfig.validateRequestTypesOnly).thenReturn(true)

  val finalVersionHeaderValue = "final"

  override def beforeEach(): Unit = {
    reset(mockV2JsonValidationService)
    reset(mockV2XmlValidationService)
    reset(mockV2BusinessValidationService)
    reset(mockV3JsonValidationService)
    reset(mockV3XmlValidationService)
    reset(mockV3BusinessValidationService)
    super.beforeEach()
  }

  val v2ValidCode: V2MessageType = V2MessageType.DeclarationData
  val v3ValidCode: V3MessageType = V3MessageType.DeclarationData
  val invalidCode                = "dummy"

  "when APIVersion is invalid or missing" - {
    lazy val validXml: NodeSeq = <test></test>
    val source                 = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))

    def reqWithAccept(accept: Option[String]): FakeRequest[Source[ByteString, ?]] =
      FakeRequest(
        method = "POST",
        uri = "/",
        headers = FakeHeaders(
          accept
            .map(
              ac => Seq("APIVersion" -> ac)
            )
            .getOrElse(Seq.empty)
        ),
        body = source
      )

    "return NOT_ACCEPTABLE when APIVersion is None" in {

      val messageType        = Gen.alphaNumStr.sample.getOrElse("messageType")
      val messagesController =
        new MessagesController(
          stubControllerComponents(),
          mockV2XmlValidationService,
          mockV3XmlValidationService,
          mockV2JsonValidationService,
          mockV3JsonValidationService,
          mockV2BusinessValidationService,
          mockV3BusinessValidationService,
          new ValidateAcceptRefiner(stubControllerComponents()),
          mockConfig
        )

      val request = reqWithAccept(None)

      val result = messagesController.validate(messageType)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"An Accept Header is missing.",
        "code"    -> "NOT_ACCEPTABLE"
      )

      status(result) mustEqual NOT_ACCEPTABLE

    }
    "return UNSUPPORTED_MEDIA_TYPE_ERROR when the APIVersion is invalid or not supported" in {

      val invalidAPIVersion  = Gen.alphaNumStr.sample.getOrElse("invalidAPIVersion")
      val messageType        = Gen.alphaNumStr.sample.getOrElse("messageType")
      val messagesController =
        new MessagesController(
          stubControllerComponents(),
          mockV2XmlValidationService,
          mockV3XmlValidationService,
          mockV2JsonValidationService,
          mockV3JsonValidationService,
          mockV2BusinessValidationService,
          mockV3BusinessValidationService,
          new ValidateAcceptRefiner(stubControllerComponents()),
          mockConfig
        )

      val request = reqWithAccept(Some(invalidAPIVersion))

      val result = messagesController.validate(messageType)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"The Accept header $invalidAPIVersion is not supported.",
        "code"    -> "UNSUPPORTED_MEDIA_TYPE"
      )

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE

    }
    "return UNSUPPORTED_MEDIA_TYPE_ERROR when the APIVersion is empty" in {

      val emptyAPIVersion    = ""
      val messageType        = Gen.alphaNumStr.sample.getOrElse("messageType")
      val messagesController =
        new MessagesController(
          stubControllerComponents(),
          mockV2XmlValidationService,
          mockV3XmlValidationService,
          mockV2JsonValidationService,
          mockV3JsonValidationService,
          mockV2BusinessValidationService,
          mockV3BusinessValidationService,
          new ValidateAcceptRefiner(stubControllerComponents()),
          mockConfig
        )

      val request = reqWithAccept(Some(emptyAPIVersion))

      val result = messagesController.validate(messageType)(request)

      contentAsJson(result) mustBe Json.obj(
        "message" -> s"The Accept header $emptyAPIVersion is not supported.",
        "code"    -> "UNSUPPORTED_MEDIA_TYPE"
      )

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE

    }
  }
  "when APIVersion is 2.1" - {
    "On validate XML" - {
      lazy val validXml: NodeSeq = <test></test>

      "on a valid XML file, with the application/xml content type, return No Content" in forAll(Gen.oneOf(V2MessageType.requestValues)) {
        messageType =>
          when(mockV2XmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
            .thenAnswer(
              _ => EitherT.rightT[Future, ValidationError](())
            )
          when(
            mockV2BusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext])
          )
            .thenAnswer(
              _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
            )

          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              mockConfig
            )

          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${messageType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(messageType.code)(request)

          status(result) mustBe NO_CONTENT
      }

      "on a valid XML file, but no valid message type, return BadRequest with an error message" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$invalidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
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
        Gen.oneOf(V2MessageType.responseValues)
      ) {
        responseType =>
          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              mockConfig
            )
          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${responseType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(responseType.code)(request)

          contentAsJson(result) mustBe Json.obj(
            "message" -> s"Unknown Message Type provided: ${responseType.code} is not recognised",
            "code"    -> "NOT_FOUND"
          )
          status(result) mustBe NOT_FOUND
      }

      "on a valid XML file for a request or response type where response types are enabled, return No Content" in forAll(
        Gen.oneOf(V2MessageType.values)
      ) {
        messageType =>
          when(mockV2XmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
            .thenAnswer(
              _ => EitherT.rightT[Future, ValidationError](())
            )
          when(
            mockV2BusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext])
          )
            .thenAnswer(
              _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
            )

          val config = mock[AppConfig]
          when(config.validateRequestTypesOnly).thenReturn(false)

          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              config
            )

          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${messageType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(messageType.code)(request)

          status(result) mustBe NO_CONTENT
      }

      "on an invalid XML file, return Ok with a list of errors" in {
        val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

        when(mockV2XmlValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, Unit](ValidationError.XmlFailedValidation(errorList))
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "validationErrors" -> Json.toJson(errorList)
        )
        status(result) mustBe OK
      }

      "on an exception being thrown during xml schema validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV2XmlValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error)))
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "root node doesn't match messageType in XML file, return BadRequest with an error message" in {
        when(mockV2XmlValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              (
                EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")),
                Flow[ByteString]
              )
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe OK

        contentAsJson(result) mustBe
          Json.obj(
            "message" -> Json.toJson("Root node doesn't match with the messageType"),
            "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
          )
      }

      "on an exception being thrown during xml business validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")

        when(mockV2XmlValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error))), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

    "On validate JSON" - {

      lazy val validJson: String   = "{}"
      lazy val invalidJson: String = "{"

      "on a valid JSON file, with the application/json content type, return No Content" in {
        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe NO_CONTENT
      }

      "on a valid JSON file, but no valid message type, return BadRequest with an error message" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$invalidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
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

        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, ValidationError](ValidationError.JsonFailedValidation(errorList))
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(invalidJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "validationErrors" -> Json.toJson(errorList)
        )
        status(result) mustBe OK
      }

      "on receiving an invalid content type, must return Unsupported Media Type" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.TEXT)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type text/plain is not supported.")
        status(result) mustBe UNSUPPORTED_MEDIA_TYPE
      }

      "with no content type header, must return Unsupported Media Type" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest("POST", s"/messages/$v2ValidCode/validate/", FakeHeaders(Seq("APIVersion" -> "2.1")), source)
        val result  = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type must be specified.")
        status(result) mustBe UNSUPPORTED_MEDIA_TYPE
      }

      "on an exception being thrown during json validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "on an JsonParseException being thrown during json validation, must return Bad Request" in {
        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              EitherT.leftT[Future, ValidationError](
                ValidationError.FailedToParse(
                  "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
                )
              )
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "code" -> "BAD_REQUEST",
          "message" -> "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
        )
        status(result) mustBe BAD_REQUEST
      }

      "root node doesn't match messageType in json, return BadRequest with an error message" in {

        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              (
                EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")),
                Flow[ByteString]
              )
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe OK

        contentAsJson(result) mustBe
          Json.obj(
            "message" -> Json.toJson("Root node doesn't match with the messageType"),
            "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
          )
      }

      "on an exception being thrown during json business validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV2JsonValidationService.validate(eqTo(v2ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV2BusinessValidationService.businessValidationFlow(eqTo(v2ValidCode), eqTo(V2MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error))), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
  }
  "when APIVersion is 3.0" - {
    "On validate XML" - {
      lazy val validXml: NodeSeq = <test></test>

      "on a valid XML file, with the application/xml content type, return No Content" in forAll(Gen.oneOf(V3MessageType.requestValues)) {
        messageType =>
          when(mockV3XmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
            .thenAnswer(
              _ => EitherT.rightT[Future, ValidationError](())
            )
          when(
            mockV3BusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext])
          )
            .thenAnswer(
              _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
            )

          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              mockConfig
            )

          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${messageType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(messageType.code)(request)

          status(result) mustBe NO_CONTENT
      }

      "on a valid XML file, but no valid message type, return BadRequest with an error message" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$invalidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
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
        Gen.oneOf(V2MessageType.responseValues)
      ) {
        responseType =>
          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              mockConfig
            )
          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${responseType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(responseType.code)(request)

          contentAsJson(result) mustBe Json.obj(
            "message" -> s"Unknown Message Type provided: ${responseType.code} is not recognised",
            "code"    -> "NOT_FOUND"
          )
          status(result) mustBe NOT_FOUND
      }

      "on a valid XML file for a request or response type where response types are enabled, return No Content" in forAll(
        Gen.oneOf(V3MessageType.values)
      ) {
        messageType =>
          when(mockV3XmlValidationService.validate(eqTo(messageType), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
            .thenAnswer(
              _ => EitherT.rightT[Future, ValidationError](())
            )
          when(
            mockV3BusinessValidationService.businessValidationFlow(eqTo(messageType), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext])
          )
            .thenAnswer(
              _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
            )

          val config = mock[AppConfig]
          when(config.validateRequestTypesOnly).thenReturn(false)

          val messagesController =
            new MessagesController(
              stubControllerComponents(),
              mockV2XmlValidationService,
              mockV3XmlValidationService,
              mockV2JsonValidationService,
              mockV3JsonValidationService,
              mockV2BusinessValidationService,
              mockV3BusinessValidationService,
              new ValidateAcceptRefiner(stubControllerComponents()),
              config
            )

          val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
          val request =
            FakeRequest(
              "POST",
              s"/messages/${messageType.code}/validate/",
              FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
              source
            )
          val result = messagesController.validate(messageType.code)(request)

          status(result) mustBe NO_CONTENT
      }

      "on an invalid XML file, return Ok with a list of errors" in {
        val errorList = NonEmptyList(XmlSchemaValidationError(1, 1, "text1"), List(XmlSchemaValidationError(2, 2, "text2")))

        when(mockV3XmlValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, Unit](ValidationError.XmlFailedValidation(errorList))
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "validationErrors" -> Json.toJson(errorList)
        )
        status(result) mustBe OK
      }

      "on an exception being thrown during xml schema validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV3XmlValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error)))
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "root node doesn't match messageType in XML file, return BadRequest with an error message" in {
        when(mockV3XmlValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              (
                EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")),
                Flow[ByteString]
              )
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe OK

        contentAsJson(result) mustBe
          Json.obj(
            "message" -> Json.toJson("Root node doesn't match with the messageType"),
            "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
          )
      }

      "on an exception being thrown during xml business validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")

        when(mockV3XmlValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Xml))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error))), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.XML)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

    "On validate JSON" - {

      lazy val validJson: String   = "{}"
      lazy val invalidJson: String = "{"

      "on a valid JSON file, with the application/json content type, return No Content" in {
        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe NO_CONTENT
      }

      "on a valid JSON file, but no valid message type, return BadRequest with an error message" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$invalidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.JSON)),
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

        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, ValidationError](ValidationError.JsonFailedValidation(errorList))
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(invalidJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "validationErrors" -> Json.toJson(errorList)
        )
        status(result) mustBe OK
      }

      "on receiving an invalid content type, must return Unsupported Media Type" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v2ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "2.1", CONTENT_TYPE -> MimeTypes.TEXT)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type text/plain is not supported.")
        status(result) mustBe UNSUPPORTED_MEDIA_TYPE
      }

      "with no content type header, must return Unsupported Media Type" in {
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest("POST", s"/messages/$v2ValidCode/validate/", FakeHeaders(Seq("APIVersion" -> "2.1")), source)
        val result  = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "UNSUPPORTED_MEDIA_TYPE", "message" -> "Content type must be specified.")
        status(result) mustBe UNSUPPORTED_MEDIA_TYPE
      }

      "on an exception being thrown during json validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.leftT[Future, ValidationError](ValidationError.Unexpected(Some(error)))
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )
        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "on an JsonParseException being thrown during json validation, must return Bad Request" in {
        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              EitherT.leftT[Future, ValidationError](
                ValidationError.FailedToParse(
                  "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
                )
              )
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.rightT[Future, ValidationError](()), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj(
          "code" -> "BAD_REQUEST",
          "message" -> "Unexpected character (''' (code 39)): was expecting double-quote to start field name\n at [Source: (akka.stream.impl.io.InputStreamAdapter); line: 1, column: 3]"
        )
        status(result) mustBe BAD_REQUEST
      }

      "root node doesn't match messageType in json, return BadRequest with an error message" in {

        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ =>
              (
                EitherT.leftT[Future, ValidationError](ValidationError.BusinessValidationError("Root node doesn't match with the messageType")),
                Flow[ByteString]
              )
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        status(result) mustBe OK

        contentAsJson(result) mustBe
          Json.obj(
            "message" -> Json.toJson("Root node doesn't match with the messageType"),
            "code"    -> Json.toJson("BUSINESS_VALIDATION_ERROR")
          )
      }

      "on an exception being thrown during json business validation, must return Internal Server Error" in {
        val error = new IllegalStateException("Unable to extract schema")
        when(mockV3JsonValidationService.validate(eqTo(v3ValidCode), any[Source[ByteString, ?]])(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => EitherT.rightT[Future, ValidationError](())
          )
        when(mockV3BusinessValidationService.businessValidationFlow(eqTo(v3ValidCode), eqTo(V3MessageFormat.Json))(any[Materializer], any[ExecutionContext]))
          .thenAnswer(
            _ => (EitherT.leftT[Future, Unit](ValidationError.Unexpected(Some(error))), Flow[ByteString])
          )

        val messagesController =
          new MessagesController(
            stubControllerComponents(),
            mockV2XmlValidationService,
            mockV3XmlValidationService,
            mockV2JsonValidationService,
            mockV3JsonValidationService,
            mockV2BusinessValidationService,
            mockV3BusinessValidationService,
            new ValidateAcceptRefiner(stubControllerComponents()),
            mockConfig
          )

        val source  = Source.single(ByteString(validJson, StandardCharsets.UTF_8))
        val request = FakeRequest(
          "POST",
          s"/messages/$v3ValidCode/validate/",
          FakeHeaders(Seq("APIVersion" -> "3.0", CONTENT_TYPE -> MimeTypes.JSON)),
          source
        )
        val result = messagesController.validate(v2ValidCode.code)(request)

        contentAsJson(result) mustBe Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
  }

}
