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

sealed trait MessageTypeXml {
  def code: String
  def rootNode: String
  def xsdPath: String
}

sealed abstract class DepartureMessageType(val code: String, val rootNode: String, val xsdPath: String) extends MessageTypeXml

sealed abstract class ArrivalMessageType(val code: String, val rootNode: String, val xsdPath: String) extends MessageTypeXml

object MessageTypeXml {

  // *******************
  // Departures Requests
  // *******************

//  /** E_DEC_AMD (IE013) */
//  case object DeclarationAmendment
//    extends DepartureMessageType("IE013", "CC013C", "/xsd/cc013c.xsd")
//
//  /** E_DEC_INV (IE014) */
//  case object DeclarationInvalidation
//    extends DepartureMessageType("IE014", "CC014C", "/xsd/cc014c.xsd")

  /** E_DEC_DAT (IE015) */
  case object DeclarationData extends DepartureMessageType("IE015", "CC015C", "/xsd/cc015c.xsd")

//  /** E_REQ_REL (IE054) */
//  case object RequestOfRelease
//    extends DepartureMessageType("IE054", "CC054C", "/xsd/cc054c.xsd")
//
//  /** E_PRE_NOT (IE170) */
//  case object PresentationNotification
//    extends DepartureMessageType("IE170", "CC170C", "/xsd/cc170c.xsd")

  val departureValues = Set(
//    DeclarationAmendment,
//    DeclarationInvalidation,
    DeclarationData
//    RequestOfRelease,
//    PresentationNotification
  )

  // ****************
  // Arrival Requests
  // ****************

  /** E_REQ_REL (IE054) */
  case object ArrivalNotification extends ArrivalMessageType("IE007", "CC007C", "/xsd/cc007c.xsd")

  /** E_PRE_NOT (IE170) */
  case object UnloadingRemarks extends ArrivalMessageType("IE044", "CC044C", "/xsd/cc044c.xsd")

  val arrivalValues = Set(
    ArrivalNotification,
    UnloadingRemarks
  )

  val values = arrivalValues ++ departureValues

}
