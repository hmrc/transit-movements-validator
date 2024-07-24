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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.models.errors

import cats.data.NonEmptyList

sealed trait ValidationError

sealed trait FailedValidationError extends ValidationError {
  val errors: NonEmptyList[SchemaValidationError]
}

object ValidationError {

  case class Unexpected(thr: Option[Throwable]) extends ValidationError

  case class UnknownMessageType(messageType: String) extends ValidationError

  case class FailedToParse(message: String) extends ValidationError

  case class XmlFailedValidation(errors: NonEmptyList[XmlSchemaValidationError]) extends FailedValidationError

  case class JsonFailedValidation(errors: NonEmptyList[JsonSchemaValidationError]) extends FailedValidationError

  case class BusinessValidationError(message: String) extends ValidationError

  case class MissingElementError(path: Seq[String]) extends ValidationError

  case class TooManyElementsError(path: Seq[String]) extends ValidationError
}
