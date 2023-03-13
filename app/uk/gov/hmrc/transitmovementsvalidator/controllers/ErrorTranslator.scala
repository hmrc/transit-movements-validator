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
import uk.gov.hmrc.transitmovementsvalidator.models.errors.FailedValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.InternalServiceError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError.badRequestError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError.internalServiceError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError.notFoundError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError.schemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ObjectStoreError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait ErrorTranslator {

  implicit class ErrorConverter[E, A](value: EitherT[Future, E, A]) {

    def asPresentation(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, PresentationError, A] =
      value.leftMap(c.convert)
  }

  trait Converter[E] {
    def convert(input: E): PresentationError
  }

  implicit val validationErrorConverter: Converter[ValidationError] = {
    case error: FailedValidationError                    => schemaValidationError(error.errors)
    case ValidationError.Unexpected(thr)                 => InternalServiceError(cause = thr)
    case ValidationError.UnknownMessageType(messageType) => notFoundError(s"Unknown Message Type provided: $messageType is not recognised")
    case ValidationError.FailedToParse(message)          => badRequestError(message)
  }

  implicit val objectStoreErrorConverter: Converter[ObjectStoreError] = {
    case ObjectStoreError.FileNotFound(fileLocation) => badRequestError(s"File not found at location: $fileLocation")
    case ObjectStoreError.UnexpectedError(thr)       => internalServiceError(cause = thr)
  }

}
