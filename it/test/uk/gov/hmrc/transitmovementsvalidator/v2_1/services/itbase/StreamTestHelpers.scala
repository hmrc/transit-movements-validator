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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.services.itbase

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import scala.xml.NodeSeq

object StreamTestHelpers extends StreamTestHelpers

trait StreamTestHelpers {

  def createStream(node: NodeSeq): Source[ByteString, ?] = createStream(node.mkString)

  def createStream(jsValue: JsValue): Source[ByteString, ?] = createStream(Json.stringify(jsValue))

  def createStream(string: String): Source[ByteString, ?] =
    Source.single(ByteString(string, StandardCharsets.UTF_8))

}
