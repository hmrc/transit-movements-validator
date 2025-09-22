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
  case class DeclarationAmendment(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE013", "CC013C", s"/${versionHeader.value}/xsd/cc013c.xsd", s"/${versionHeader.value}/json/cc013c-schema.json")
      with DepartureRequestMessageType

  /** E_DEC_INV (IE014) */
  case class DeclarationInvalidation(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE014", "CC014C", s"/${versionHeader.value}/xsd/cc014c.xsd", s"/${versionHeader.value}/json/cc014c-schema.json")
      with DepartureRequestMessageType

  /** E_DEC_DAT (IE015) */
  case class DeclarationData(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE015", "CC015C", s"/${versionHeader.value}/xsd/cc015c.xsd", s"/${versionHeader.value}/json/cc015c-schema.json")
      with DepartureRequestMessageType

  /** E_PRE_NOT (IE170) */
  case class PresentationNotificationForPreLodgedDec(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE170", "CC170C", s"/${versionHeader.value}/xsd/cc170c.xsd", s"/${versionHeader.value}/json/cc170c-schema.json")
      with DepartureRequestMessageType

  def departureRequestValues(version: APIVersionHeader): Set[MessageType] = Set(
    DeclarationAmendment(version),
    DeclarationInvalidation(version),
    DeclarationData(version),
    PresentationNotificationForPreLodgedDec(version)
  )

  // ****************
  // Arrival Requests
  // ****************

  /** E_ARR_NOT (IE007) */
  case class ArrivalNotification(versionHeader: APIVersionHeader)
      extends ArrivalMessageType("IE007", "CC007C", s"/${versionHeader.value}/xsd/cc007c.xsd", s"/${versionHeader.value}/json/cc007c-schema.json")
      with ArrivalRequestMessageType

  /** E_ULD_REM (IE044) */
  case class UnloadingRemarks(versionHeader: APIVersionHeader)
      extends ArrivalMessageType("IE044", "CC044C", s"/${versionHeader.value}/xsd/cc044c.xsd", s"/${versionHeader.value}/json/cc044c-schema.json")
      with ArrivalRequestMessageType

  def arrivalRequestValues(version: APIVersionHeader): Set[MessageType] = Set(
    ArrivalNotification(version),
    UnloadingRemarks(version)
  )

  // *****
  // Departure Responses
  // *****

  case class AmendmentAcceptance(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE004", "CC004C", s"/${versionHeader.value}/xsd/cc004c.xsd", s"/${versionHeader.value}/json/cc004c-schema.json")
      with ResponseMessageType

  case class InvalidationDecision(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE009", "CC009C", s"/${versionHeader.value}/xsd/cc009c.xsd", s"/${versionHeader.value}/json/cc009c-schema.json")
      with ResponseMessageType
  case class Discrepancies(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE019", "CC019C", s"/${versionHeader.value}/xsd/cc019c.xsd", s"/${versionHeader.value}/json/cc019c-schema.json")
      with ResponseMessageType
  case class MRNAllocated(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE028", "CC028C", s"/${versionHeader.value}/xsd/cc028c.xsd", s"/${versionHeader.value}/json/cc028c-schema.json")
      with ResponseMessageType

  case class ReleaseForTransit(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE029", "CC029C", s"/${versionHeader.value}/xsd/cc029c.xsd", s"/${versionHeader.value}/json/cc029c-schema.json")
      with ResponseMessageType

  case class RecoveryNotification(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE035", "CC035C", s"/${versionHeader.value}/xsd/cc035c.xsd", s"/${versionHeader.value}/json/cc035c-schema.json")
      with ResponseMessageType

  case class WriteOffNotification(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE045", "CC045C", s"/${versionHeader.value}/xsd/cc045c.xsd", s"/${versionHeader.value}/json/cc045c-schema.json")
      with ResponseMessageType

  case class NoReleaseForTransit(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE051", "CC051C", s"/${versionHeader.value}/xsd/cc051c.xsd", s"/${versionHeader.value}/json/cc051c-schema.json")
      with ResponseMessageType

  case class GuaranteeNotValid(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE055", "CC055C", s"/${versionHeader.value}/xsd/cc055c.xsd", s"/${versionHeader.value}/json/cc055c-schema.json")
      with ResponseMessageType

  case class RejectionFromOfficeOfDeparture(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE056", "CC056C", s"/${versionHeader.value}/xsd/cc056c.xsd", s"/${versionHeader.value}/json/cc056c-schema.json")
      with ResponseMessageType

  case class ControlDecisionNotification(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE060", "CC060C", s"/${versionHeader.value}/xsd/cc060c.xsd", s"/${versionHeader.value}/json/cc060c-schema.json")
      with ResponseMessageType

  case class ForwardedIncidentNotificationToED(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE182", "CC182C", s"/${versionHeader.value}/xsd/cc182c.xsd", s"/${versionHeader.value}/json/cc182c-schema.json")
      with ResponseMessageType
  case class FunctionalNack(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE906", "CC906C", s"/${versionHeader.value}/xsd/cc906c.xsd", s"/${versionHeader.value}/json/cc906c-schema.json")
      with ResponseMessageType

  case class PositiveAcknowledge(versionHeader: APIVersionHeader)
      extends DepartureMessageType("IE928", "CC928C", s"/${versionHeader.value}/xsd/cc928c.xsd", s"/${versionHeader.value}/json/cc928c-schema.json")
      with ResponseMessageType

  def departureResponseValues(version: APIVersionHeader): Set[MessageType] = Set(
    AmendmentAcceptance(version),
    InvalidationDecision(version),
    Discrepancies(version),
    MRNAllocated(version),
    ReleaseForTransit(version),
    RecoveryNotification(version),
    WriteOffNotification(version),
    NoReleaseForTransit(version),
    GuaranteeNotValid(version),
    RejectionFromOfficeOfDeparture(version),
    ControlDecisionNotification(version),
    ForwardedIncidentNotificationToED(version),
    FunctionalNack(version),
    PositiveAcknowledge(version)
  )

  // ****
  // Arrival Responses
  // ***

  case class GoodsReleaseNotification(version: APIVersionHeader)
      extends ArrivalMessageType("IE025", "CC025C", s"/${version.value}/xsd/cc025c.xsd", s"/${version.value}/json/cc025c-schema.json")
      with ResponseMessageType

  case class UnloadingPermission(versionHeader: APIVersionHeader)
      extends ArrivalMessageType("IE043", "CC043C", s"/${versionHeader.value}/xsd/cc043c.xsd", s"/${versionHeader.value}/json/cc043c-schema.json")
      with ResponseMessageType

  case class RejectionFromOfficeOfDestination(versionHeader: APIVersionHeader)
      extends ArrivalMessageType("IE057", "CC057C", s"/${versionHeader.value}/xsd/cc057c.xsd", s"/${versionHeader.value}/json/cc057c-schema.json")
      with ResponseMessageType

  def arrivalResponseValues(version: APIVersionHeader): Set[MessageType] = Set(
    GoodsReleaseNotification(version),
    UnloadingPermission(version),
    RejectionFromOfficeOfDestination(version),
    FunctionalNack(version),
    PositiveAcknowledge(version)
  )

  def requestValues(version: APIVersionHeader): Set[MessageType]  = arrivalRequestValues(version) ++ departureRequestValues(version)
  def responseValues(version: APIVersionHeader): Set[MessageType] = arrivalResponseValues(version) ++ departureResponseValues(version)

  def values(version: APIVersionHeader): Set[MessageType] = requestValues(version) ++ responseValues(version)

  def find(code: String, requestOnly: Boolean, version: APIVersionHeader): Option[MessageType] =
    (if (requestOnly) requestValues(version) else values(version)).find(_.code == code)

}
