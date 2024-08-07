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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.models

sealed trait MessageType {
  def code: String
  def rootNode: String
  def xsdPath: String
  def jsonSchemaPath: String
}

sealed trait RequestMessageType extends MessageType {
  def routingOfficeNode: String
}

sealed trait DepartureRequestMessageType extends RequestMessageType {
  override lazy val routingOfficeNode: String = "CustomsOfficeOfDeparture"
}

sealed trait ArrivalRequestMessageType extends RequestMessageType {
  override lazy val routingOfficeNode: String = "CustomsOfficeOfDestinationActual"
}

sealed trait ResponseMessageType extends MessageType

sealed abstract class DepartureMessageType(val code: String, val rootNode: String, val xsdPath: String, val jsonSchemaPath: String) extends MessageType

sealed abstract class ArrivalMessageType(val code: String, val rootNode: String, val xsdPath: String, val jsonSchemaPath: String) extends MessageType

object MessageType {

  // *******************
  // Departures Requests
  // *******************

  /** E_DEC_AMD (IE013) */
  case object DeclarationAmendment
      extends DepartureMessageType("IE013", "CC013C", "/v2_1/xsd/cc013c.xsd", "/v2_1/json/cc013c-schema.json")
      with DepartureRequestMessageType

  /** E_DEC_INV (IE014) */
  case object DeclarationInvalidation
      extends DepartureMessageType("IE014", "CC014C", "/v2_1/xsd/cc014c.xsd", "/v2_1/json/cc014c-schema.json")
      with DepartureRequestMessageType

  /** E_DEC_DAT (IE015) */
  case object DeclarationData
      extends DepartureMessageType("IE015", "CC015C", "/v2_1/xsd/cc015c.xsd", "/v2_1/json/cc015c-schema.json")
      with DepartureRequestMessageType

  /** E_PRE_NOT (IE170) */
  case object PresentationNotificationForPreLodgedDec
      extends DepartureMessageType("IE170", "CC170C", "/v2_1/xsd/cc170c.xsd", "/v2_1/json/cc170c-schema.json")
      with DepartureRequestMessageType

  val departureRequestValues: Set[MessageType] = Set(
    DeclarationAmendment,
    DeclarationInvalidation,
    DeclarationData,
    PresentationNotificationForPreLodgedDec
  )

  // ****************
  // Arrival Requests
  // ****************

  /** E_ARR_NOT (IE007) */
  case object ArrivalNotification
      extends ArrivalMessageType("IE007", "CC007C", "/v2_1/xsd/cc007c.xsd", "/v2_1/json/cc007c-schema.json")
      with ArrivalRequestMessageType

  /** E_ULD_REM (IE044) */
  case object UnloadingRemarks
      extends ArrivalMessageType("IE044", "CC044C", "/v2_1/xsd/cc044c.xsd", "/v2_1/json/cc044c-schema.json")
      with ArrivalRequestMessageType

  val arrivalRequestValues: Set[MessageType] = Set(
    ArrivalNotification,
    UnloadingRemarks
  )

  // *****
  // Departure Responses
  // *****

  case object AmendmentAcceptance
      extends DepartureMessageType("IE004", "CC004C", "/v2_1/xsd/cc004c.xsd", "/v2_1/json/cc004c-schema.json")
      with ResponseMessageType

  case object InvalidationDecision
      extends DepartureMessageType("IE009", "CC009C", "/v2_1/xsd/cc009c.xsd", "/v2_1/json/cc009c-schema.json")
      with ResponseMessageType
  case object Discrepancies extends DepartureMessageType("IE019", "CC019C", "/v2_1/xsd/cc019c.xsd", "/v2_1/json/cc019c-schema.json") with ResponseMessageType
  case object MRNAllocated  extends DepartureMessageType("IE028", "CC028C", "/v2_1/xsd/cc028c.xsd", "/v2_1/json/cc028c-schema.json") with ResponseMessageType

  case object ReleaseForTransit
      extends DepartureMessageType("IE029", "CC029C", "/v2_1/xsd/cc029c.xsd", "/v2_1/json/cc029c-schema.json")
      with ResponseMessageType

  case object RecoveryNotification
      extends DepartureMessageType("IE035", "CC035C", "/v2_1/xsd/cc035c.xsd", "/v2_1/json/cc035c-schema.json")
      with ResponseMessageType

  case object WriteOffNotification
      extends DepartureMessageType("IE045", "CC045C", "/v2_1/xsd/cc045c.xsd", "/v2_1/json/cc045c-schema.json")
      with ResponseMessageType

  case object NoReleaseForTransit
      extends DepartureMessageType("IE051", "CC051C", "/v2_1/xsd/cc051c.xsd", "/v2_1/json/cc051c-schema.json")
      with ResponseMessageType

  case object GuaranteeNotValid
      extends DepartureMessageType("IE055", "CC055C", "/v2_1/xsd/cc055c.xsd", "/v2_1/json/cc055c-schema.json")
      with ResponseMessageType

  case object RejectionFromOfficeOfDeparture
      extends DepartureMessageType("IE056", "CC056C", "/v2_1/xsd/cc056c.xsd", "/v2_1/json/cc056c-schema.json")
      with ResponseMessageType

  case object ControlDecisionNotification
      extends DepartureMessageType("IE060", "CC060C", "/v2_1/xsd/cc060c.xsd", "/v2_1/json/cc060c-schema.json")
      with ResponseMessageType

  case object ForwardedIncidentNotificationToED
      extends DepartureMessageType("IE182", "CC182C", "/v2_1/xsd/cc182c.xsd", "/v2_1/json/cc182c-schema.json")
      with ResponseMessageType
  case object FunctionalNack extends DepartureMessageType("IE906", "CC906C", "/v2_1/xsd/cc906c.xsd", "/v2_1/json/cc906c-schema.json") with ResponseMessageType

  case object PositiveAcknowledge
      extends DepartureMessageType("IE928", "CC928C", "/v2_1/xsd/cc928c.xsd", "/v2_1/json/cc928c-schema.json")
      with ResponseMessageType

  val departureResponseValues: Set[MessageType] = Set(
    AmendmentAcceptance,
    InvalidationDecision,
    Discrepancies,
    MRNAllocated,
    ReleaseForTransit,
    RecoveryNotification,
    WriteOffNotification,
    NoReleaseForTransit,
    GuaranteeNotValid,
    RejectionFromOfficeOfDeparture,
    ControlDecisionNotification,
    ForwardedIncidentNotificationToED,
    FunctionalNack,
    PositiveAcknowledge
  )

  // ****
  // Arrival Responses
  // ***

  case object GoodsReleaseNotification
      extends ArrivalMessageType("IE025", "CC025C", "/v2_1/xsd/cc025c.xsd", "/v2_1/json/cc025c-schema.json")
      with ResponseMessageType

  case object UnloadingPermission
      extends ArrivalMessageType("IE043", "CC043C", "/v2_1/xsd/cc043c.xsd", "/v2_1/json/cc043c-schema.json")
      with ResponseMessageType

  case object RejectionFromOfficeOfDestination
      extends ArrivalMessageType("IE057", "CC057C", "/v2_1/xsd/cc057c.xsd", "/v2_1/json/cc057c-schema.json")
      with ResponseMessageType

  val arrivalResponseValues: Set[MessageType] = Set(
    GoodsReleaseNotification,
    UnloadingPermission,
    RejectionFromOfficeOfDestination,
    FunctionalNack,
    PositiveAcknowledge
  )

  val requestValues  = arrivalRequestValues ++ departureRequestValues
  val responseValues = arrivalResponseValues ++ departureResponseValues

  val values: Set[MessageType] = requestValues ++ responseValues

  def find(code: String, requestOnly: Boolean): Option[MessageType] = (if (requestOnly) requestValues else values).find(_.code == code)

}
