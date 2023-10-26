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

import akka.NotUsed
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
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Zip
import akka.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import uk.gov.hmrc.transitmovementsvalidator.config.AppConfig
import uk.gov.hmrc.transitmovementsvalidator.models.CustomsOffice
import uk.gov.hmrc.transitmovementsvalidator.models.MessageFormat
import uk.gov.hmrc.transitmovementsvalidator.models.MessageType
import uk.gov.hmrc.transitmovementsvalidator.models.RequestMessageType
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

/** Akka Streams implementation of business validation rules.
  *
  * The intention behind this service is to provide a [[Flow]] that takes in a
  * [[ByteString]] and returns a [[ByteString]] that can be used for schema validation, while
  * simultaneously performing business validation.
  *
  * ==How this service works==
  *
  * This service provides a [[Flow]] to perform **deferred validation**, that is, calling this
  * service doesn't perform the validation itself, the flow you obtain has to be attached to another
  * Akka [[akka.stream.scaladsl.Source]], i.e. almost always going to ne the [[play.api.mvc.Request]]
  * body.
  *
  * When this flow is attached to a stream and the stream is subsequently executed, this flow will parse
  * the stream as it is consumed by its ultimate consumer (the XML/Json parsers) in parallel. It does
  * not create an additional copy of the raw data, though it may create tokens to represent the data.
  * The way the tokens are parsed and strings are extracted are determined by the [[MessageFormat]]
  * that we have ingested.
  *
  * When everything is hooked up, the flow will look something like this:
  *
  * {{{
  * incoming stream -----> schema validation sink -> schema validation result
  *                   |
  *                  \/
  *        business validation flow
  *                  \/
  *             token parser* --> messageTypeRule -> merge results -> business validation result (as pre-mat Future)
  *                            |                       /\
  *                            --> officeCheckRule ----|
  * }}}
  *
  * (* -- parsing done via the supplied MessageFormat)
  *
  * Because this service doesn't actually do the validation eagerly, we have to provide a
  * [[Future]] that we can inspect when the flow is actually executed, i.e., when the
  * schema validation is performed, thus this service returns a tuple of this future,
  * and the flow to attach to an existing source.
  *
  * ==Adding new rules==
  *
  * All rules need to be a Flow of A to ValidationError, which only emits if there is an error.
  * Once created, each rule needs to go into the rules seq in businessValidationFlow.
  */
class BusinessValidationServiceImpl @Inject() (appConfig: AppConfig) extends BusinessValidationService {

  private val gbOffice = "^GB.*$".r
  private val xiOffice = "^XI.*$".r

  private val bypassValidationFlow: Flow[Any, ValidationError, NotUsed] = Flow.fromSinkAndSource(Sink.ignore, Source.empty[ValidationError])

  private def messageTypeLocation(messageType: MessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: "messageType" :: Nil

  private def officeLocation(messageType: RequestMessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: messageType.routingOfficeNode :: "referenceNumber" :: Nil

  private def recipientLocation(messageType: MessageType, messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(messageType) :: "messageRecipient" :: Nil

  private def lrnLocation(messageFormat: MessageFormat[_]): Seq[String] =
    messageFormat.rootNode(MessageType.DeclarationData) :: "TransitOperation" :: "LRN" :: Nil

  /** Ensure that one, and only one, element is returned, returning an error if zero or more than one
    * has been emitted.
    *
    * This is for use in fold.
    *
    * @param path The path to add to the error message
    * @param current The current element
    * @param next The next element
    * @return The [[ValidationError]], if there is one
    */
  def single(path: Seq[String])(current: Option[ValidationError], next: Option[ValidationError]): Option[ValidationError] =
    current match {
      case Some(_: MissingElementError)      => next
      case x @ Some(_: TooManyElementsError) => x
      case _                                 => Some(TooManyElementsError(path))
    }

  /** Ensure that one, and only one, element is returned, returning an error if zero or more than one
    * has been emitted.
    *
    * This is for use in fold.
    *
    * @param path    The path to add to the error message
    * @param current The current element
    * @param next    The next element
    * @return The [[ValidationError]], if there is one, else the value of the element
    */
  def singleEither[T](path: Seq[String])(current: Either[ValidationError, T], next: Either[ValidationError, T]): Either[ValidationError, T] =
    current match {
      case Left(_: MissingElementError)      => next
      case x @ Left(_: TooManyElementsError) => x
      case _                                 => Left(TooManyElementsError(path))
    }

  // Rules

  /** This rule checks the "messageType" field in the XML/Json and returns if it's not the value that
    * is expected (it should match the message type we're trying to validate).
    *
    * This flow will only emit an element if there is a validation error.
    *
    * @param messageType The [[MessageType]]
    * @param messageFormat The format of the message (XML or Json)
    * @tparam A The type of token
    * @return A flow that returns a [[ValidationError]] if there is a validation error, else does not emit anything
    */
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

  /** This rule checks the "LRN" field in the IE015 XML/Json and returns if it doesn't match the supplied regex, if
    * LRN validation was enabled.
    *
    * This flow will only emit an element if there is a validation error.
    *
    * @param messageType   The [[MessageType]]
    * @param messageFormat The format of the message (XML or Json)
    * @tparam A The type of token
    * @return A flow that returns a [[ValidationError]] if there is a validation error, else does not emit anything
    */
  private def checkLRNForDepartures[A](messageType: MessageType, messageFormat: MessageFormat[A]): Flow[A, ValidationError, _] =
    if (appConfig.validateLrnEnabled && MessageType.departureRequestValues.contains(messageType)) {
      val path = lrnLocation(messageFormat)
      messageFormat
        .stringValueFlow(path)
        .via(
          Flow.fromFunction[String, Option[BusinessValidationError]] {
            string =>
              if (appConfig.validateLrnRegex.matches(string.trim)) None
              else
                Some(
                  BusinessValidationError(
                    s"LRN must match the regex ${appConfig.validateLrnRegex.regex}, but '${string.trim}' was provided"
                  )
                )
          }
        )
        .filter(_.isDefined)
        .map(_.get)
    } else bypassValidationFlow

  /** Extracts the office as specified in [[RequestMessageType.routingOfficeNode]]
    *
    * @param messageType The [[MessageType]] to get the path from
    * @param messageFormat The format of the incoming message
    * @tparam A The type of the tokens
    * @return A flow that can extract the office, or returns an error
    */
  private def officeFlow[A](messageType: RequestMessageType, messageFormat: MessageFormat[A]): Flow[A, Either[ValidationError, CustomsOffice], _] = {
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

  /** Extracts the message recipient
    *
    * @param messageType   The [[MessageType]]
    * @param messageFormat The format of the incoming message
    * @tparam A The type of the tokens
    * @return A flow that can extract the recipient, or returns an error
    */
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

  /** Composes the checkOffice and checkRecipient flows, and ensures that the countries extracted from each matches.
    *
    * If they do, this flow will return no elements, else this will return a [[ValidationError]]
    *
    * * if not GB/XI office, error as in checkOffice
    * * if recipient not NTA.(XI|GB), error as in checkRecipient
    * * if both valid GB/XI but not matching, error that states they don't match
    *
    * It does this by creating two flows that run in parallel, one to get the message recipient country,
    * one to get the office of destination/departure as appropriate, passes the country on if successful,
    * then the two streams zip the results together and if the two countries match, emits nothing, else
    * an appropriate error is generated.
    *
    * Zip turns two elements from two streams into a tuple of the two, i.e. the transformation [A, B => (A, B)],
    * which allows us to use both elements in the next step, and ensure we only get one element.
    *
    * The graph looks like this
    *
    * {{{
    *   in -> tokens --> broadcast ---> extractOffice -------> zip -> calculate result ---> error, if any -> out
    *                       |                            |
    *                       |-> extractMessageRecipient -|
    * }}}
    *
    * @param messageType   The [[MessageType]]
    * @param messageFormat The format of the incoming message
    * @tparam A The type of the tokens
    * @return A flow that returns a [[ValidationError]] if there is a validation error, else does not emit anything
    */
  def checkOfficeAndRecipient[A](messageType: RequestMessageType, messageFormat: MessageFormat[A]): Flow[A, ValidationError, _] =
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

  /** Creates and returns a pre-materialised flow for checking business rules.
    *
    * Add rules to the "rules" Seq. This will configure the graph accordingly.
    *
    * @param messageType The [[MessageType]]
    * @param messageFormat The [[MessageFormat]] to parse
    * @param materializer The [[Materializer]] that will pre-materialise the stream
    * @param ec The [[ExecutionContext]]
    * @tparam A The type of token that [[MessageFormat]] will parse the stream into
    * @return The pre-materialised [[Future]] and the [[Flow]].
    */
  override def businessValidationFlow[A](
    messageType: MessageType,
    messageFormat: MessageFormat[A]
  )(implicit materializer: Materializer, ec: ExecutionContext): (EitherT[Future, ValidationError, Unit], Flow[ByteString, ByteString, _]) = {
    // The rule selected here is controlled by configuration and is only applicable for messages from the Trader
    val checkOfficeRule = messageType match {
      case requestMessageType: RequestMessageType => checkOfficeAndRecipient(requestMessageType, messageFormat)
      case _                                      => bypassValidationFlow
    }

    // Add each rule here, this will take care of all configuration needed in the graph below.
    // Each rule MUST be a Flow[A, ValidationError, _], which will emit ZERO elements on success
    val rules: Seq[Flow[A, ValidationError, _]] = Seq(
      checkMessageType(messageType, messageFormat),
      checkOfficeRule,
      checkLRNForDepartures(messageType, messageFormat)
    )

    // This sink ensure we don't error the stream if something goes wrong and take down the whole thing
    // Otherwise, this populates the future returned from the graph
    val recoverableSink: Sink[ValidationError, Future[Option[ValidationError]]] = Flow
      .apply[ValidationError]
      .recover {
        case NonFatal(ex) => ValidationError.Unexpected(Some(ex))
      }
      .toMat(Sink.headOption)(Keep.right)

    /*
     * This graph looks like this:
     *
     * in -> initialBroadcast -> output (this is "flow", for attaching to source)
     *       |
     *       |->  tokenParser -> rulesBroadcast (-> ruleFlow ->) merge -> sink (gets first error, if any)
     *
     * (-> ruleFlow ->) will be as many rules as there are, specified above. The sink will put its result
     * into the Future returned by preMat.
     */
    val (preMat, flow): (Future[Option[ValidationError]], Flow[ByteString, ByteString, _]) = Flow
      .fromGraph(
        GraphDSL.createGraph(recoverableSink) {
          implicit builder => sink =>
            import GraphDSL.Implicits._

            val initialBroadcast = builder.add(Broadcast[ByteString](2))
            val parser           = builder.add(messageFormat.tokenParser)

            val numberOfRules = rules.length
            val ruleBroadcast = builder.add(Broadcast[A](numberOfRules))
            val merge         = builder.add(Concat[ValidationError](numberOfRules)) // Concat for order guarantees

            initialBroadcast.out(1) ~> parser ~> ruleBroadcast.in

            // Adds all rules to the broadcast
            rules.foreach {
              rule =>
                ruleBroadcast ~> builder.add(rule) ~> merge
            }

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

    // We return a tuple of:
    //
    // * An EitherT[Future, ValidationError, Unit] that holds the result of the graph
    //   above once it is run
    // * The one-time use flow to use on the request source that will then be sent to
    //   the schema validator
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
