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
import play.api.mvc.Headers
import uk.gov.hmrc.transitmovementsvalidator.controllers.ObjectStoreURIExtractHeader.expectedUriPattern
import uk.gov.hmrc.transitmovementsvalidator.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError

import scala.concurrent.Future
import scala.util.matching.Regex

object ObjectStoreURIExtractHeader {

  // The URI consists of the service name in the first part of the path, followed
  // by the location of the object in the context of that service. As this service
  // targets common-transit-convention-traders' objects exclusively, we ensure
  // the URI is targeting that context. This regex ensures that this is the case.
  val expectedUriPattern: Regex = "^common-transit-convention-traders/(.+)$".r
}

trait ObjectStoreURIHeaderExtractor {

  def extractObjectStoreURI(headers: Headers): EitherT[Future, PresentationError, ObjectStoreResourceLocation] =
    EitherT(
      Future.successful(
        for {
          headerValue                 <- getHeader(headers.get("X-Object-Store-Uri"))
          objectStoreResourceLocation <- getObjectStoreResourceLocation(headerValue)
        } yield ObjectStoreResourceLocation(objectStoreResourceLocation)
      )
    )

  def getHeader(objectStoreURI: Option[String]) =
    objectStoreURI.toRight(PresentationError.badRequestError("Missing X-Object-Store-Uri header value"))

  def getObjectStoreResourceLocation(headerValue: String) =
    expectedUriPattern
      .findFirstMatchIn(headerValue)
      .map(_.group(1))
      .toRight(PresentationError.badRequestError(s"X-Object-Store-Uri header value does not start with common-transit-convention-traders/ (got $headerValue)"))
}
