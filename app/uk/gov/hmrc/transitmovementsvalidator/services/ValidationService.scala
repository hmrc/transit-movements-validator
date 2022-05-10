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

package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import com.google.inject.Singleton

import scala.concurrent.Future

@ImplementedBy(classOf[ValidationServiceImpl])
trait ValidationService {

  // TODO: fix signature to be representative of what we want
  //  We need a much better object to return - but for now, validation errors on the right, can't parse errors on left.
  def validateXML(messageType: String, source: Source[ByteString, _])(implicit materializer: Materializer): Future[Either[String, Seq[String]]]

}

@Singleton
class ValidationServiceImpl extends ValidationService {

  override def validateXML(messageType: String, source: Source[ByteString, _])(implicit
    materializer: Materializer
  ): Future[Either[String, Seq[String]]] = {
    source.runWith(Sink.ignore)
    Future.successful(Right(Seq.empty))
  }
}
