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

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.Object
import uk.gov.hmrc.objectstore.client.ObjectMetadata
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.Path.File
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.transitmovementsvalidator.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ObjectStoreError.FileNotFound
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ObjectStoreError.UnexpectedError

import java.time.Instant
import java.util.UUID.randomUUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ObjectStoreServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with ScalaCheckDrivenPropertyChecks {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockClient: PlayObjectStoreClient = mock[PlayObjectStoreClient]

  lazy val filePath = ObjectStoreResourceLocation(
    Path
      .Directory(s"common-transit-convention-traders/movements/12345678")
      .file(randomUUID.toString)
      .asUri
  )
  private val fileContents = "<xml>content</xml>"

  "ObjectStoreService" - {
    "should return the file contents" in {
      val metadata = ObjectMetadata("", 0, Md5Hash(""), Instant.now(), Map.empty[String, String])
      val obj      = Option[Object[Source[ByteString, NotUsed]]](Object.apply(File(filePath.value), Source.single(ByteString(fileContents)), metadata))
      when(mockClient.getObject[Source[ByteString, NotUsed]](any[File](), any())(any(), any())).thenReturn(Future.successful(obj))

      val sut    = new ObjectStoreServiceImpl(mockClient)
      val result = sut.getContents(filePath).value

      whenReady(result) {
        _ mustBe Right(obj.get.content)
      }
    }

    "should return an error when the file is not found in object store" in {
      when(mockClient.getObject[Source[ByteString, NotUsed]](any[File](), any())(any(), any())).thenReturn(Future.successful(None))
      val sut    = new ObjectStoreServiceImpl(mockClient)
      val result = sut.getContents(filePath).value

      whenReady(result) {
        case Left(FileNotFound(fileName)) => succeed
        case reason                       => fail(s"Expected Left(FileNotFound), got $reason")
      }
    }

    "should return an error when there is a problem getting an object from object store" in {
      when(mockClient.getObject[Source[ByteString, NotUsed]](any[File](), any())(any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR)))
      val sut    = new ObjectStoreServiceImpl(mockClient)
      val result = sut.getContents(filePath).value

      whenReady(result) {
        case Left(UnexpectedError(fileName)) => succeed
        case reason                          => fail(s"Expected an UnexpectedError, got $reason")
      }
    }

  }
}
