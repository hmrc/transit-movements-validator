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

package uk.gov.hmrc.transitmovementsvalidator.models

import akka.NotUsed
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.alpakka.xml.ParseEvent
import akka.stream.alpakka.xml.TextEvent
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.jsfr.json.path.JsonPath
import play.api.libs.json.{Json => PlayJson}
import play.api.libs.json.JsString

import scala.annotation.tailrec

sealed trait MessageFormat[A] {
  def rootNode(messageType: MessageType): String
  def tokenParser: Flow[ByteString, A, _]
  def stringValueFlow(path: Seq[String]): Flow[A, String, _]
}

object MessageFormat {

  case object Xml extends MessageFormat[ParseEvent] {
    override def rootNode(messageType: MessageType): String = messageType.rootNode

    override def stringValueFlow(path: Seq[String]): Flow[ParseEvent, String, _] =
      XmlParsing
        .subslice(path)
        .mapConcat {
          case event: TextEvent => Seq(event.text)
          case _                => Seq.empty
        }

    override val tokenParser: Flow[ByteString, ParseEvent, NotUsed] = XmlParsing.parser
  }

  case object Json extends MessageFormat[ByteString] {

    override def rootNode(messageType: MessageType): String = s"n1:${messageType.rootNode}"

    override def stringValueFlow(path: Seq[String]): Flow[ByteString, String, _] = {
      @tailrec
      def createPath(path: Seq[String], acc: JsonPath.Builder = JsonPath.Builder.start()): JsonPath =
        (path: @unchecked) match { // we know it won't be anything else
          case Nil          => acc.build()
          case head :: tail => createPath(tail, acc.child(head))
        }

      JsonReader
        .select(createPath(path))
        .via(Flow.fromFunction {
          fragment =>
            PlayJson.parse(fragment.utf8String)
        })
        .via(Flow.fromFunction {
          entry =>
            (entry: @unchecked) match { // if it's something else, the schema validator will catch it too
              case JsString(value) => value
            }
        })
    }

    override val tokenParser: Flow[ByteString, ByteString, _] = Flow[ByteString] // no-op for json
  }

}
