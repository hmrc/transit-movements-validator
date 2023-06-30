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

import akka.stream.Attributes
import akka.stream.Attributes.LogLevels
import akka.stream.FlowShape
import akka.stream.Materializer
import akka.stream.scaladsl.Broadcast
import akka.stream.scaladsl.Concat
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Zip
import akka.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.CustomsOffice
import uk.gov.hmrc.transitmovementsvalidator.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.BusinessValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.MissingElementError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError.TooManyElementsError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

@ImplementedBy(classOf[BusinessValidationServiceImpl])
trait BusinessValidationService {

  def businessValidationFlow[A](messageType: MessageType, messageFormat: MessageFormat[A])(implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): (EitherT[Future, ValidationError, Unit], Flow[ByteString, ByteString, _])

}

class BusinessValidationServiceImpl @Inject() (appConfig: AppConfig) extends BusinessValidationService {

  private val gbOffice = "^GB.*$".r
  private val xiOffice = "^XI.*$".r

  private def messageTypeLocation(messageType: MessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: "messageType" :: Nil

  private def officeLocation(messageType: MessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: messageType.routingOfficeNode :: "referenceNumber" :: Nil

  private def recipientLocation(messageType: MessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: "messageRecipient" :: Nil

  def single(path: Seq[String])(current: Option[ValidationError], next: Option[ValidationError]): Option[ValidationError] =
    current match {
      case Some(_: MissingElementError)      => next
      case x @ Some(_: TooManyElementsError) => x
      case _                                 => Some(TooManyElementsError(path))
    }

  def singleEither[T](path: Seq[String])(current: Either[ValidationError, T], next: Either[ValidationError, T]): Either[ValidationError, T] =
    current match {
      case Left(_: MissingElementError)      => next
      case x @ Left(_: TooManyElementsError) => x
      case _                                 => Left(TooManyElementsError(path))
    }

  // Rules
  private def checkMessageType[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, ValidationError, _] = {
    val path = messageTypeLocation(messageType, messageFormat)
    messageFormat
      .stringValueFlow(path)
      .via(
        Flow.fromFunction[String, Option[BusinessValidationError]] {
          string =>
            if (string.trim == messageType.rootNode) None
            else
              Some(
                BusinessValidationError(
                  s"Root node doesn't match with the messageType"
                )
              )
        }
      )
      .fold[Option[ValidationError]](Some(MissingElementError(path)))(single(path))
      .filter(_.isDefined)
      .map(_.get)

  }

  private def officeFlow[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, Either[ValidationError, CustomsOffice], _] = {
    val path = officeLocation(messageType, messageFormat)
    messageFormat
      .stringValueFlow(path)
      .via(
        Flow.fromFunction[String, Either[BusinessValidationError, CustomsOffice]] {
          case gbOffice() => Right(CustomsOffice.Gb)
          case xiOffice() => Right(CustomsOffice.Xi)
          case string =>
            Left(
              BusinessValidationError(
                s"The customs office specified for ${messageType.routingOfficeNode} must be a customs office located in the United Kingdom ($string was specified)"
              )
            )
        }
      )
      .fold[Either[ValidationError, CustomsOffice]](Left(MissingElementError(path)))(singleEither(path))
  }

  private def recipientFlow[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, Either[ValidationError, CustomsOffice], _] = {
    val path = recipientLocation(messageType, messageFormat)
    messageFormat
      .stringValueFlow(path)
      .via(
        Flow.fromFunction[String, Either[BusinessValidationError, CustomsOffice]] {
          case "NTA.GB" => Right(CustomsOffice.Gb)
          case "NTA.XI" => Right(CustomsOffice.Xi)
          case string =>
            Left(
              BusinessValidationError(
                s"The message recipient must be either NTA.GB or NTA.XI ($string was specified)"
              )
            )
        }
      )
      .fold[Either[ValidationError, CustomsOffice]](Left(MissingElementError(path)))(singleEither(path))
  }

  // determine office, determine recipient
  // if not GB/XI office, error as in checkOffice
  // if recipient not NTA.(XI|GB), error as appropriate
  // if both valid GB/XI but not matching, appropriate error
  def checkOfficeAndRecipient[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, ValidationError, _] =
    Flow
      .fromGraph(
        GraphDSL.create() {
          implicit builder =>
            import GraphDSL.Implicits._

            val broadcast = builder.add(Broadcast[A](2))
            val zip       = builder.add(Zip[Either[ValidationError, CustomsOffice], Either[ValidationError, CustomsOffice]]())
            val office    = builder.add(officeFlow(messageType, messageFormat))
            val recipient = builder.add(recipientFlow(messageType, messageFormat))
            val process =
              builder.add(Flow.fromFunction[(Either[ValidationError, CustomsOffice], Either[ValidationError, CustomsOffice]), Option[ValidationError]] {
                case (Right(left), Right(right)) =>
                  if (left == right) None
                  else Some(BusinessValidationError(s"The message recipient country must match the country of the ${messageType.routingOfficeNode}"))
                case (Left(error), _) => Some(error)
                case (_, Left(error)) => Some(error)
              })

            broadcast.out(0) ~> office ~> zip.in0
            broadcast.out(1) ~> recipient ~> zip.in1

            zip.out ~> process.in

            FlowShape(broadcast.in, process.out)
        }
      )
      .take(1)
      .filter(_.isDefined)
      .map(_.get)

  private def checkOffice[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, ValidationError, _] = {
    val path = officeLocation(messageType, messageFormat)
    officeFlow(messageType, messageFormat)
      .map(_.swap.toOption)
      .fold[Option[ValidationError]](Some(MissingElementError(path)))((single(path)))
      .filter(_.isDefined)
      .map(_.get)
  }

  override def businessValidationFlow[A](
    messageType: MessageType,
    messageFormat: MessageFormat[A]
  )(implicit materializer: Materializer, ec: ExecutionContext): (EitherT[Future, ValidationError, Unit], Flow[ByteString, ByteString, _]) = {
    val recoverableSink: Sink[ValidationError, Future[Option[ValidationError]]] = Flow
      .apply[ValidationError]
      .recover {
        case NonFatal(ex) => ValidationError.Unexpected(Some(ex))
      }
      .toMat(Sink.headOption)(Keep.right)
    val (preMat, flow): (Future[Option[ValidationError]], Flow[ByteString, ByteString, _]) = Flow
      .fromGraph(
        GraphDSL.createGraph(recoverableSink) {
          implicit builder => sink =>
            import GraphDSL.Implicits._

            val initialBroadcast = builder.add(Broadcast[ByteString](2))
            val xmlParser        = builder.add(messageFormat.tokenParser)
            val ruleBroadcast    = builder.add(Broadcast[A](2))
            val merge            = builder.add(Concat[ValidationError](2)) // for order guarantees

            // Business rules to check
            val messageTypeRule = builder.add(checkMessageType(messageType, messageFormat))
            val checkOfficeRule =
              if (appConfig.enableBusinessValidationMessageRecipient) builder.add(checkOfficeAndRecipient(messageType, messageFormat))
              else builder.add(checkOffice(messageType, messageFormat))

            initialBroadcast.out(1) ~> xmlParser ~> ruleBroadcast.in

            ruleBroadcast.out(0) ~> messageTypeRule ~> merge.in(0)
            ruleBroadcast.out(1) ~> checkOfficeRule ~> merge.in(1)

            merge.out ~> sink

            FlowShape(initialBroadcast.in, initialBroadcast.out(0))
        }
      )
      .withAttributes(
        Attributes.logLevels(
          onFailure = LogLevels.Off,
          onFinish = LogLevels.Off
        )
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

}
