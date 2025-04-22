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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.stream

import cats.implicits.catsSyntaxEitherId
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc.BaseControllerHelpers
import play.api.mvc.BodyParser

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

trait StreamingParsers {
  self: BaseControllerHelpers =>

  implicit val materializer: Materializer

  /*
    This keeps Play's connection thread pool outside of our streaming, and uses a cached thread pool
    to spin things up as needed. Additional defence against performance issues picked up in CTCP-1545.
   */
  implicit val materializerExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  lazy val streamFromMemory: BodyParser[Source[ByteString, ?]] = BodyParser {
    _ =>
      Accumulator.source[ByteString].map(_.asRight)(materializer.executionContext)
  }
}
