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

package uk.gov.hmrc.transitmovementsvalidator.models

sealed trait MessageTypeJson {
  def code: String
  def schemaPath: String
}

sealed abstract class DepartureMessageTypeJson(val code: String, val schemaPath: String) extends MessageTypeJson

sealed abstract class ArrivalMessageTypeJson(val code: String, val schemaPath: String) extends MessageTypeJson

object MessageTypeJson {

  // *******************
  // Departures Requests
  // *******************

  //  /** E_DEC_AMD (IE013) */
  //  case object DeclarationAmendment
  //    extends DepartureMessageType("IE013", "CC013C", "/json/cc013c.xsd")
  //
  //  /** E_DEC_INV (IE014) */
  //  case object DeclarationInvalidation
  //    extends DepartureMessageType("IE014", "CC014C", "/json/cc014c.xsd")

  /** E_DEC_DAT (IE015) */
  case object DeclarationDataJson extends DepartureMessageTypeJson("IE015", "./conf/json/cc015c.json")

  //  /** E_REQ_REL (IE054) */
  //  case object RequestOfRelease
  //    extends DepartureMessageType("IE054", "CC054C", "/json/cc054c.xsd")
  //
  //  /** E_PRE_NOT (IE170) */
  //  case object PresentationNotification
  //    extends DepartureMessageType("IE170", "CC170C", "/json/cc170c.xsd")

  val departureValues = Set(
    //    DeclarationAmendment,
    //    DeclarationInvalidation,
    DeclarationDataJson
    //    RequestOfRelease,
    //    PresentationNotification
  )

  // ****************
  // Arrival Requests
  // ****************

//  /** E_REQ_REL (IE054) */
//  case object ArrivalNotification extends ArrivalMessageType("IE007", "/json/cc007c.json")
//
//  /** E_PRE_NOT (IE170) */
//  case object UnloadingRemarks extends ArrivalMessageType("IE044", "/json/cc044c.json")
//
//  val arrivalValues = Set(
//    ArrivalNotification,
//    UnloadingRemarks
//  )

  val values = departureValues

}
