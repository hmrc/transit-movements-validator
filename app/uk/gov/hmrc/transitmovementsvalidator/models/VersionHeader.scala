/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsvalidator.models

sealed trait VersionHeader {
  val value: String
}

object VersionHeader {

  def apply(maybeVersionHeaderValue: Option[String]): VersionHeader =
    maybeVersionHeaderValue.map(_.trim.toLowerCase) match {
      case Some(Final.value)        => Final
      case Some(Transitional.value) => Transitional
      case Some(invalid)            => InvalidVersionHeader(invalid)
      case None                     => Transitional
    }
}

case object Final extends VersionHeader {
  override val value: String = "final"
}

case object Transitional extends VersionHeader {
  override val value: String = "transitional"
}

final case class InvalidVersionHeader(value: String) extends VersionHeader
