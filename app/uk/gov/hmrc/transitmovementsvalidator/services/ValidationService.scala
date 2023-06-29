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

import akka.stream.FlowShape
import akka.stream.Materializer
import akka.stream.scaladsl.Broadcast
import akka.stream.scaladsl.Concat
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.MissingElementError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.TooManyElementsError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait ValidationService {

  protected def rootNode(messageType: MessageType): String = messageType.rootNode

  def messageTypeLocation(messageType: MessageType): Seq[String] = rootNode(messageType) :: "messageType" :: Nil

  def officeLocation(messageType: MessageType): Seq[String] = rootNode(messageType) :: messageType.routingOfficeNode :: "referenceNumber" :: Nil

  protected def stringValueFlow(path: Seq[String]): Flow[ByteString, String, _]

  def single(path: Seq[String])(current: Option[ValidationError], next: Option[ValidationError]): Option[ValidationError] =
    current match {
      case Some(_: MissingElementError)      => next
      case x @ Some(_: TooManyElementsError) => x
      case _                                 => Some(TooManyElementsError(path))
    }

  // Rules
  def checkMessageType(messageType: MessageType): Flow[ByteString, ValidationError, _] = {
    val path = messageTypeLocation(messageType)
    stringValueFlow(path)
      .via(
        Flow.fromFunction[String, Option[BusinessValidationError]](
          string =>
            if (string.trim == messageType.rootNode) None
            else
              Some(
                BusinessValidationError(
                  s"Root node doesn't match with the messageType"
                )
              )
        )
      )
      .fold[Option[ValidationError]](Some(MissingElementError(path)))(single(path))
      .filter(_.isDefined)
      .map(_.get)

  }

  def checkOffice(messageType: MessageType): Flow[ByteString, ValidationError, _] = {
    val path = officeLocation(messageType)
    stringValueFlow(path)
      .via(
        Flow.fromFunction[String, Option[BusinessValidationError]](
          string =>
            if (string.startsWith("GB") || string.startsWith("XI")) None
            else
              Some(
                BusinessValidationError(
                  s"The customs office specified for ${messageType.routingOfficeNode} must be a customs office located in the United Kingdom ($string was specified)"
                )
              )
        )
      )
      .fold[Option[ValidationError]](Some(MissingElementError(path)))((single(path)))
      .filter(_.isDefined)
      .map(_.get)
  }

  def businessValidationFlow(
    messageType: MessageType
  )(implicit materializer: Materializer, ec: ExecutionContext): (EitherT[Future, ValidationError, Unit], Flow[ByteString, ByteString, _]) = {
    val (preMat, flow): (Future[Option[ValidationError]], Flow[ByteString, ByteString, _]) = Flow
      .fromGraph(
        GraphDSL.createGraph(Sink.headOption[ValidationError]) {
          implicit builder => sink =>
            import GraphDSL.Implicits._

            val broadcast = builder.add(Broadcast[ByteString](3))
            val merge     = builder.add(Concat[ValidationError](2)) // for order guarantees

            // Business rules to check
            val messageTypeRule = builder.add(checkMessageType(messageType))
            val checkOfficeRule = builder.add(checkOffice(messageType))

            broadcast.out(1) ~> messageTypeRule ~> merge.in(0)
            broadcast.out(2) ~> checkOfficeRule ~> merge.in(1)

            merge.out ~> sink

            FlowShape(broadcast.in, broadcast.out(0))
        }
      )
      .preMaterialize()
    (
      EitherT(
        preMat.map(
          x => x.map(Left.apply).getOrElse(Right((): Unit))
        )
      ),
      flow
    )
  }

  def validate(messageType: MessageType, source: Source[ByteString, _])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): EitherT[Future, ValidationError, Unit]

}
