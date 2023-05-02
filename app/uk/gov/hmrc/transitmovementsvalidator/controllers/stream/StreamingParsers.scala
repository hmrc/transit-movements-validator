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

package uk.gov.hmrc.transitmovementsvalidator.controllers.stream

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import cats.implicits.catsSyntaxMonadError
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, ActionBuilder, BaseControllerHelpers, BodyParser, Result}
import uk.gov.hmrc.transitmovementsvalidator.controllers.BodyReplaceableRequest

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

trait StreamingParsers {
  self: BaseControllerHelpers =>

  implicit val materializer: Materializer

  /*
    This keeps Play's connection thread pool outside of our streaming, and uses a cached thread pool
    to spin things up as needed. Additional defence against performance issues picked up in CTCP-1545.
   */
  implicit val materializerExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  lazy val streamFromMemory: BodyParser[Source[ByteString, _]] = BodyParser {
    _ =>
      Accumulator.source[ByteString].map(Right.apply)(materializer.executionContext)
  }

  implicit class ActionBuilderStreamHelpers[R[A] <: BodyReplaceableRequest[R, A]](actionBuilder: ActionBuilder[R, _]) {

    /** Updates the [[Source]] in the [[BodyReplaceableRequest]] with a version that can be used
     * multiple times via the use of a temporary file.
     *
     * @param block The code to use the with the reusable source
     * @return An [[Action]]
     */
    def stream(
                block: R[Source[ByteString, _]] => Future[Result]
              )(implicit temporaryFileCreator: TemporaryFileCreator): Action[Source[ByteString, _]] =
      actionBuilder.async(streamFromMemory) {
        request =>
          val file = temporaryFileCreator.create()
          (for {
            _ <- request.body.runWith(FileIO.toPath(file))
            result <- block(request.replaceBody(FileIO.fromPath(file)))
          } yield result)
            .attemptTap {
              _ =>
                file.delete()
                Future.successful(())
            }
      }
  }

}
