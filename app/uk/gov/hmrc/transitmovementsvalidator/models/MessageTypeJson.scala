package uk.gov.hmrc.transitmovementsvalidator.models

sealed trait MessageTypeJson {
  def code: String
  def schemaPath: String
}

sealed abstract class DepartureMessageType(val code: String, val schemaPath: String) extends MessageTypeJson

sealed abstract class ArrivalMessageType(val code: String, val schemaPath: String) extends MessageTypeJson

object MessageTypeJson {

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
  case object DeclarationData extends DepartureMessageType("IE015", "/jsonSchema/cc015c.json")

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

//  /** E_REQ_REL (IE054) */
//  case object ArrivalNotification extends ArrivalMessageType("IE007", "/jsonSchema/cc007c.json")
//
//  /** E_PRE_NOT (IE170) */
//  case object UnloadingRemarks extends ArrivalMessageType("IE044", "/jsonSchema/cc044c.json")
//
//  val arrivalValues = Set(
//    ArrivalNotification,
//    UnloadingRemarks
//  )

  val values = departureValues

}
