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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.config

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

import scala.util.matching.Regex

@Singleton
class AppConfig @Inject() (config: Configuration) {
  lazy val appName: String = config.get[String]("appName")

  lazy val validateRequestTypesOnly: Boolean = config.getOptional[Boolean]("validate-request-types-only").getOrElse(true)

  lazy val validateLrnEnabled: Boolean = config.get[Boolean]("validate-lrn.enabled")
  lazy val validateLrnRegex: Regex     = config.get[String]("validate-lrn.regex").r
}
