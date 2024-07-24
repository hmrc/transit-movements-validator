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

package uk.gov.hmrc.transitmovementsvalidator.routing

import cats.implicits.catsSyntaxOptionId
import cats.implicits.none
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.BaseController
import play.api.mvc.Request
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.Final
import uk.gov.hmrc.transitmovementsvalidator.models.InvalidVersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.Transitional
import uk.gov.hmrc.transitmovementsvalidator.models.VersionHeader
import uk.gov.hmrc.transitmovementsvalidator.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsvalidator.stream.StreamingParsers

import scala.concurrent.Future

trait VersionedRouting {
  self: BaseController with StreamingParsers =>

  def apiVersionRoute(routes: PartialFunction[Option[String], Action[_]])(implicit
    materializer: Materializer,
    appConfig: AppConfig
  ): Action[Source[ByteString, _]] =
    Action.async(streamFromMemory) {
      (request: Request[Source[ByteString, _]]) =>
        val maybeVersionedHeader = VersionHeader.apply(request.headers.get(appConfig.versionHeaderKey)) match {
          case version @ Final         => version.value.some
          case version @ Transitional  => version.value.some
          case InvalidVersionHeader(_) => none
        }

        routes
          .lift(maybeVersionedHeader)
          .map(
            action => action(request).run(request.body)
          )
          .getOrElse {
            request.body.to(Sink.ignore).run()
            val invalidHeader = request.headers.get(appConfig.versionHeaderKey).getOrElse("")
            Future.successful(
              BadRequest(
                Json.toJson(
                  PresentationError.badRequestError(s"Unsupported header APIVersion: '$invalidHeader'")
                )
              )
            )
          }
    }
}
