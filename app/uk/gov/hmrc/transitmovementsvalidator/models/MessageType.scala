package uk.gov.hmrc.transitmovementsvalidator.models

sealed trait MessageType extends Product with Serializable {
  def code: String
  def rootNode: String
  def xsdPath: String
}

case class DepartureMessageType(code: String, rootNode: String, xsdPath: String) extends MessageType

case class ArrivalMessageType(code: String, rootNode: String, xsdPath: String) extends MessageType

object MessageType {

  // *******************
  // Departures Requests
  // *******************

  /** E_DEC_AMD (IE013) */
  case object DeclarationAmendment
    extends DepartureMessageType("IE013", "CC013C", "/xsd/CC013C.xsd")

  /** E_DEC_INV (IE014) */
  case object DeclarationInvalidation
    extends DepartureMessageType("IE014", "CC014C", "/xsd/CC014C.xsd")

  /** E_DEC_DAT (IE015) */
  case object DeclarationData
    extends DepartureMessageType("IE015", "CC015C", "/xsd/CC015C.xsd")

  /** E_REQ_REL (IE054) */
  case object RequestOfRelease
    extends DepartureMessageType("IE054", "CC054C", "/xsd/CC054C.xsd")

  /** E_PRE_NOT (IE170) */
  case object PresentationNotification
    extends DepartureMessageType("IE170", "CC170C", "/xsd/CC170C.xsd")

  val departureValues = Set(
    DeclarationAmendment,
    DeclarationInvalidation,
    DeclarationData,
    RequestOfRelease,
    PresentationNotification
  )

  // ****************
  // Arrival Requests
  // ****************

  /** E_REQ_REL (IE054) */
  case object ArrivalNotification
    extends ArrivalMessageType("IE007", "CC007C", "/xsd/CC007C.xsd")

  /** E_PRE_NOT (IE170) */
  case object UnloadingRemarks
    extends ArrivalMessageType("IE044", "CC044C", "/xsd/CC044C.xsd")

  val arrivalValues = Set(
    ArrivalNotification,
    UnloadingRemarks
  )

  val values = arrivalValues ++ departureValues

}
