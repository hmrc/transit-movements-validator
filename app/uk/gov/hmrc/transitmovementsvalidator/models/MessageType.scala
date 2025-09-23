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
  case class DeclarationAmendment(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE013",
        "CC013C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc013c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc013c-schema.json"
      )
      with DepartureRequestMessageType

  /** E_DEC_INV (IE014) */
  case class DeclarationInvalidation(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE014",
        "CC014C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc014c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc014c-schema.json"
      )
      with DepartureRequestMessageType

  /** E_DEC_DAT (IE015) */
  case class DeclarationData(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE015",
        "CC015C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc015c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc015c-schema.json"
      )
      with DepartureRequestMessageType

  /** E_PRE_NOT (IE170) */
  case class PresentationNotificationForPreLodgedDec(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE170",
        "CC170C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc170c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc170c-schema.json"
      )
      with DepartureRequestMessageType

  def departureRequestValues(apiVersion: APIVersionHeader): Set[MessageType] = Set(
    DeclarationAmendment(apiVersion),
    DeclarationInvalidation(apiVersion),
    DeclarationData(apiVersion),
    PresentationNotificationForPreLodgedDec(apiVersion)
  )

  // ****************
  // Arrival Requests
  // ****************

  /** E_ARR_NOT (IE007) */
  case class ArrivalNotification(apiVersion: APIVersionHeader)
      extends ArrivalMessageType(
        "IE007",
        "CC007C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc007c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc007c-schema.json"
      )
      with ArrivalRequestMessageType

  /** E_ULD_REM (IE044) */
  case class UnloadingRemarks(apiVersion: APIVersionHeader)
      extends ArrivalMessageType(
        "IE044",
        "CC044C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc044c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc044c-schema.json"
      )
      with ArrivalRequestMessageType

  def arrivalRequestValues(apiVersion: APIVersionHeader): Set[MessageType] = Set(
    ArrivalNotification(apiVersion),
    UnloadingRemarks(apiVersion)
  )

  // *****
  // Departure Responses
  // *****

  case class AmendmentAcceptance(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE004",
        "CC004C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc004c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc004c-schema.json"
      )
      with ResponseMessageType

  case class InvalidationDecision(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE009",
        "CC009C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc009c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc009c-schema.json"
      )
      with ResponseMessageType
  case class Discrepancies(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE019",
        "CC019C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc019c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc019c-schema.json"
      )
      with ResponseMessageType
  case class MRNAllocated(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE028",
        "CC028C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc028c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc028c-schema.json"
      )
      with ResponseMessageType

  case class ReleaseForTransit(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE029",
        "CC029C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc029c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc029c-schema.json"
      )
      with ResponseMessageType

  case class RecoveryNotification(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE035",
        "CC035C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc035c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc035c-schema.json"
      )
      with ResponseMessageType

  case class WriteOffNotification(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE045",
        "CC045C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc045c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc045c-schema.json"
      )
      with ResponseMessageType

  case class NoReleaseForTransit(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE051",
        "CC051C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc051c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc051c-schema.json"
      )
      with ResponseMessageType

  case class GuaranteeNotValid(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE055",
        "CC055C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc055c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc055c-schema.json"
      )
      with ResponseMessageType

  case class RejectionFromOfficeOfDeparture(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE056",
        "CC056C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc056c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc056c-schema.json"
      )
      with ResponseMessageType

  case class ControlDecisionNotification(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE060",
        "CC060C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc060c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc060c-schema.json"
      )
      with ResponseMessageType

  case class ForwardedIncidentNotificationToED(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE182",
        "CC182C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc182c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc182c-schema.json"
      )
      with ResponseMessageType
  case class FunctionalNack(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE906",
        "CC906C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc906c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc906c-schema.json"
      )
      with ResponseMessageType

  case class PositiveAcknowledge(apiVersion: APIVersionHeader)
      extends DepartureMessageType(
        "IE928",
        "CC928C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc928c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc928c-schema.json"
      )
      with ResponseMessageType

  def departureResponseValues(apiVersion: APIVersionHeader): Set[MessageType] = Set(
    AmendmentAcceptance(apiVersion),
    InvalidationDecision(apiVersion),
    Discrepancies(apiVersion),
    MRNAllocated(apiVersion),
    ReleaseForTransit(apiVersion),
    RecoveryNotification(apiVersion),
    WriteOffNotification(apiVersion),
    NoReleaseForTransit(apiVersion),
    GuaranteeNotValid(apiVersion),
    RejectionFromOfficeOfDeparture(apiVersion),
    ControlDecisionNotification(apiVersion),
    ForwardedIncidentNotificationToED(apiVersion),
    FunctionalNack(apiVersion),
    PositiveAcknowledge(apiVersion)
  )

  // ****
  // Arrival Responses
  // ***

  case class GoodsReleaseNotification(apiVersion: APIVersionHeader)
      extends ArrivalMessageType(
        "IE025",
        "CC025C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc025c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc025c-schema.json"
      )
      with ResponseMessageType

  case class UnloadingPermission(apiVersion: APIVersionHeader)
      extends ArrivalMessageType(
        "IE043",
        "CC043C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc043c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc043c-schema.json"
      )
      with ResponseMessageType

  case class RejectionFromOfficeOfDestination(apiVersion: APIVersionHeader)
      extends ArrivalMessageType(
        "IE057",
        "CC057C",
        s"/${MessageType.toPath(apiVersion.value)}/xsd/cc057c.xsd",
        s"/${MessageType.toPath(apiVersion.value)}/json/cc057c-schema.json"
      )
      with ResponseMessageType

  def arrivalResponseValues(apiVersion: APIVersionHeader): Set[MessageType] = Set(
    GoodsReleaseNotification(apiVersion),
    UnloadingPermission(apiVersion),
    RejectionFromOfficeOfDestination(apiVersion),
    FunctionalNack(apiVersion),
    PositiveAcknowledge(apiVersion)
  )

  def requestValues(apiVersion: APIVersionHeader): Set[MessageType]  = arrivalRequestValues(apiVersion) ++ departureRequestValues(apiVersion)
  def responseValues(apiVersion: APIVersionHeader): Set[MessageType] = arrivalResponseValues(apiVersion) ++ departureResponseValues(apiVersion)

  def values(apiVersion: APIVersionHeader): Set[MessageType] = requestValues(apiVersion) ++ responseValues(apiVersion)

  def find(code: String, requestOnly: Boolean, apiVersion: APIVersionHeader): Option[MessageType] =
    (if (requestOnly) requestValues(apiVersion) else values(apiVersion)).find(_.code == code)

  def toPath(value: String) = s"v${value.replace('.', '_')}"
}
