/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsvalidator.controllers.stream

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.streams.Accumulator
import play.api.mvc.BaseControllerHelpers
import play.api.mvc.BodyParser

trait StreamingParsers {
  self: BaseControllerHelpers =>

  implicit val materializer: Materializer

  lazy val streamFromMemory: BodyParser[Source[ByteString, _]] = BodyParser {
    _ =>
      Accumulator.source[ByteString].map(Right.apply)(materializer.executionContext)
  }

  lazy val streamFromFile: BodyParser[Source[ByteString, _]] = {
    val tempFile = SingletonTemporaryFileCreator.create("requestBody", "asTemporaryFile")
    parse
      .file(tempFile, Long.MaxValue)
      .map(
        file => FileIO.fromPath(file.toPath)
      )(materializer.executionContext)
  }

}
