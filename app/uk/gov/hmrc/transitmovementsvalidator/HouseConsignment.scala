package uk.gov.hmrc.transitmovementsvalidator

import uk.gov.hmrc.transitmovementsvalidator.GenerateXML.xmlValue

object HouseConsignment {

  def main(args: Array[String]): Unit =
    println("HouseConsignment....") ///
  /*
   this takes 41 MB for 99 runs -->
    if we include ConsignmentItem
    for each run ConsignmentItem takes 2.34 GB
    for 99 runs  ConsignmentItem takes 231 GB  + 41 MB for HouseConsignment
   */

  val xmlValue = new StringBuilder()

  for (i <- 0 to 99) {
    xmlValue.append("<HouseConsignment>\n      <sequenceNumber>token</sequenceNumber>\n      <!--Optional:-->\n      <countryOfDispatch>st</countryOfDispatch>\n      <grossMass>1000.000000000000</grossMass>\n      <!--Optional:-->\n      <referenceNumberUCR>string</referenceNumberUCR>\n      <!--Optional:-->\n      <Consignor>\n        <!--Optional:-->\n        <identificationNumber>string</identificationNumber>\n        <!--Optional:-->\n        <name>string</name>\n        <!--Optional:-->\n        <Address>\n          <streetAndNumber>string</streetAndNumber>\n          <!--Optional:-->\n          <postcode>string</postcode>\n          <city>string</city>\n          <country>st</country>\n        </Address>\n        <!--Optional:-->\n        <ContactPerson>\n          <name>string</name>\n          <phoneNumber>token</phoneNumber>\n          <!--Optional:-->\n          <eMailAddress>string</eMailAddress>\n        </ContactPerson>\n      </Consignor>\n      <!--Optional:-->\n      <Consignee>\n        <!--Optional:-->\n        <identificationNumber>string</identificationNumber>\n        <!--Optional:-->\n        <name>string</name>\n        <!--Optional:-->\n        <Address>\n          <streetAndNumber>string</streetAndNumber>\n          <!--Optional:-->\n          <postcode>string</postcode>\n          <city>string</city>\n          <country>st</country>\n        </Address>\n      </Consignee>")

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <AdditionalSupplyChainActor>\n        <sequenceNumber>token</sequenceNumber>\n        <role>token</role>\n        <identificationNumber>string</identificationNumber>\n      </AdditionalSupplyChainActor>")
    }

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
    for (i <- 0 to 999) {
      xmlValue.append("      <DepartureTransportMeans>\n        <sequenceNumber>token</sequenceNumber>\n        <typeOfIdentification>token</typeOfIdentification>\n        <identificationNumber>string</identificationNumber>\n        <nationality>st</nationality>\n      </DepartureTransportMeans>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <PreviousDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n        <!--Optional:-->\n        <complementOfInformation>string</complementOfInformation>\n      </PreviousDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <SupportingDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n        <!--Optional:-->\n        <documentLineItemNumber>100</documentLineItemNumber>\n        <!--Optional:-->\n        <complementOfInformation>string</complementOfInformation>\n      </SupportingDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <TransportDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n      </TransportDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <AdditionalReference>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <!--Optional:-->\n        <referenceNumber>string</referenceNumber>\n      </AdditionalReference>")
    }

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <AdditionalInformation>\n        <sequenceNumber>token</sequenceNumber>\n        <code>token</code>\n        <!--Optional:-->\n        <text>string</text>\n      </AdditionalInformation>")
    }

    xmlValue.append("      <!--Optional:-->\n      <TransportCharges>\n        <methodOfPayment>s</methodOfPayment>\n      </TransportCharges>")

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
/*    for (i <- 0 to 999) {
      xmlValue.append("      <ConsignmentItem>\n        <goodsItemNumber>token</goodsItemNumber>\n        <declarationGoodsItemNumber>100</declarationGoodsItemNumber>\n        <!--Optional:-->\n        <declarationType>token</declarationType>\n        <!--Optional:-->\n        <countryOfDispatch>st</countryOfDispatch>\n        <!--Optional:-->\n        <countryOfDestination>token</countryOfDestination>\n        <!--Optional:-->\n        <referenceNumberUCR>string</referenceNumberUCR>\n        <!--Optional:-->\n        <Consignee>\n          <!--Optional:-->\n          <identificationNumber>string</identificationNumber>\n          <!--Optional:-->\n          <name>string</name>\n          <!--Optional:-->\n          <Address>\n            <streetAndNumber>string</streetAndNumber>\n            <!--Optional:-->\n            <postcode>string</postcode>\n            <city>string</city>\n            <country>st</country>\n          </Address>\n        </Consignee>")

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <AdditionalSupplyChainActor>\n          <sequenceNumber>token</sequenceNumber>\n          <role>token</role>\n          <identificationNumber>string</identificationNumber>\n        </AdditionalSupplyChainActor>")
      }

      xmlValue.append("        <Commodity>\n          <descriptionOfGoods>string</descriptionOfGoods>\n          <!--Optional:-->\n          <cusCode>token</cusCode>\n          <!--Optional:-->\n          <CommodityCode>\n            <harmonizedSystemSubHeadingCode>token</harmonizedSystemSubHeadingCode>\n            <!--Optional:-->\n            <combinedNomenclatureCode>st</combinedNomenclatureCode>\n          </CommodityCode>")
      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("<DangerousGoods>\n            <sequenceNumber>token</sequenceNumber>\n            <UNNumber>token</UNNumber>\n          </DangerousGoods>")
      }
      xmlValue.append("<!--Optional:-->\n          <GoodsMeasure>\n            <!--Optional:-->\n            <grossMass>1000.000000000000</grossMass>\n            <!--Optional:-->\n            <netMass>1000.000000000000</netMass>\n            <!--Optional:-->\n            <supplementaryUnits>1000.000000000000</supplementaryUnits>\n          </GoodsMeasure>\n        </Commodity>")

      xmlValue.append("\n<!--1 to 99 repetitions:-->\n")
      for (i <- 1 to 99) {
        xmlValue.append("        <Packaging>\n          <sequenceNumber>token</sequenceNumber>\n          <typeOfPackages>token</typeOfPackages>\n          <!--Optional:-->\n          <numberOfPackages>100</numberOfPackages>\n          <!--Optional:-->\n          <shippingMarks>string</shippingMarks>\n        </Packaging>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <PreviousDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n          <!--Optional:-->\n          <goodsItemNumber>100</goodsItemNumber>\n          <!--Optional:-->\n          <typeOfPackages>token</typeOfPackages>\n          <!--Optional:-->\n          <numberOfPackages>100</numberOfPackages>\n          <!--Optional:-->\n          <measurementUnitAndQualifier>token</measurementUnitAndQualifier>\n          <!--Optional:-->\n          <quantity>1000.000000000000</quantity>\n          <!--Optional:-->\n          <complementOfInformation>string</complementOfInformation>\n        </PreviousDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <SupportingDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n          <!--Optional:-->\n          <documentLineItemNumber>100</documentLineItemNumber>\n          <!--Optional:-->\n          <complementOfInformation>string</complementOfInformation>\n        </SupportingDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <TransportDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n        </TransportDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <AdditionalReference>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <!--Optional:-->\n          <referenceNumber>string</referenceNumber>\n        </AdditionalReference>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 99) {
        xmlValue.append("        <AdditionalInformation>\n          <sequenceNumber>token</sequenceNumber>\n          <code>token</code>\n          <!--Optional:-->\n          <text>string</text>\n        </AdditionalInformation>")
      }

      xmlValue.append("        <!--Optional:-->\n        <TransportCharges>\n          <methodOfPayment>s</methodOfPayment>\n        </TransportCharges>")

      xmlValue.append("\n</ConsignmentItem>")
    }*/

    xmlValue.append("\n</HouseConsignment>")
  }

  import java.nio.file.{Paths, Files}
  import java.nio.charset.StandardCharsets

  Files.write(Paths.get("HouseConsignment.xml"), xmlValue.toString().getBytes(StandardCharsets.UTF_8))

}
