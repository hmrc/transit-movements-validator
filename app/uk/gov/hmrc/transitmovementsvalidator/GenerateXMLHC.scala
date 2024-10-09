/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsvalidator

object GenerateXMLHC {
/*
GenerateXML --> 3.3MB
TransportEquipment --> 18GB
HouseConsignment for 99 runs --> 231GB * 20

Total -> 249 GB
 */
  def main(args: Array[String]): Unit =
    println("Hello from main of object")

  val xmlValue = new StringBuilder("<ncts:CC015C PhaseID=\"NCTS5.1\" xmlns:ncts=\"http://ncts.dgtaxud.ec\">")
  xmlValue.append("<messageSender>token</messageSender>\n  <messageRecipient>token</messageRecipient>\n  <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>\n  <messageIdentification>token</messageIdentification>\n  <messageType>CD975C</messageType>\n  <!--Optional:-->\n  <correlationIdentifier>token</correlationIdentifier>\n  <TransitOperation>\n    <LRN>string</LRN>\n    <declarationType>token</declarationType>\n    <additionalDeclarationType>token</additionalDeclarationType>\n    <!--Optional:-->\n    <TIRCarnetNumber>string</TIRCarnetNumber>\n    <!--Optional:-->\n    <presentationOfTheGoodsDateAndTime>2014-06-09T16:15:04+01:00</presentationOfTheGoodsDateAndTime>\n    <security>token</security>\n    <reducedDatasetIndicator>1</reducedDatasetIndicator>\n    <!--Optional:-->\n    <specificCircumstanceIndicator>token</specificCircumstanceIndicator>\n    <!--Optional:-->\n    <communicationLanguageAtDeparture>st</communicationLanguageAtDeparture>\n    <bindingItinerary>1</bindingItinerary>\n    <!--Optional:-->\n    <limitDate>2013-05-22+01:00</limitDate>\n  </TransitOperation>\n")

  xmlValue.append("  <!--0 to 9 repetitions:-->\n")
  for(i <- 0 to 0) {
    xmlValue.append("  <Authorisation>\n    <sequenceNumber>")
    xmlValue.append(i)
    xmlValue.append("</sequenceNumber>\n    <type>token</type>\n    <referenceNumber>string</referenceNumber>\n  </Authorisation>")
  }

  xmlValue.append("  <CustomsOfficeOfDeparture>\n    <referenceNumber>stringst</referenceNumber>\n  </CustomsOfficeOfDeparture>\n  <CustomsOfficeOfDestinationDeclared>\n    <referenceNumber>stringst</referenceNumber>\n  </CustomsOfficeOfDestinationDeclared>")

  xmlValue.append("  <!--0 to 9 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("  <CustomsOfficeOfTransitDeclared>\n    <sequenceNumber>token</sequenceNumber>\n    <referenceNumber>stringst</referenceNumber>\n    <!--Optional:-->\n    <arrivalDateAndTimeEstimated>2002-11-05T08:01:03+00:00</arrivalDateAndTimeEstimated>\n  </CustomsOfficeOfTransitDeclared>")
  }

  xmlValue.append("  <!--0 to 9 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("  <CustomsOfficeOfExitForTransitDeclared>\n    <sequenceNumber>token</sequenceNumber>\n    <referenceNumber>stringst</referenceNumber>\n  </CustomsOfficeOfExitForTransitDeclared>")
  }

  xmlValue.append("  <HolderOfTheTransitProcedure>\n    <!--Optional:-->\n    <identificationNumber>string</identificationNumber>\n    <!--Optional:-->\n    <TIRHolderIdentificationNumber>string</TIRHolderIdentificationNumber>\n    <!--Optional:-->\n    <name>string</name>\n    <!--Optional:-->\n    <Address>\n      <streetAndNumber>string</streetAndNumber>\n      <!--Optional:-->\n      <postcode>string</postcode>\n      <city>string</city>\n      <country>st</country>\n    </Address>\n    <!--Optional:-->\n    <ContactPerson>\n      <name>string</name>\n      <phoneNumber>token</phoneNumber>\n      <!--Optional:-->\n      <eMailAddress>string</eMailAddress>\n    </ContactPerson>\n  </HolderOfTheTransitProcedure>")
  xmlValue.append("  <!--Optional:-->\n  <Representative>\n    <identificationNumber>string</identificationNumber>\n    <status>token</status>\n    <!--Optional:-->\n    <ContactPerson>\n      <name>string</name>\n      <phoneNumber>token</phoneNumber>\n      <!--Optional:-->\n      <eMailAddress>string</eMailAddress>\n    </ContactPerson>\n  </Representative>")

  xmlValue.append("\n<!--1 to 9 repetitions:-->\n")
  for (i <- 1 to 1) {
    xmlValue.append("<Guarantee>\n    <sequenceNumber>token</sequenceNumber>\n    <guaranteeType>s</guaranteeType>\n    <!--Optional:-->\n    <otherGuaranteeReference>string</otherGuaranteeReference>")
    xmlValue.append("\n <!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("    <GuaranteeReference>\n      <sequenceNumber>token")
      xmlValue.append(i)
      xmlValue.append("</sequenceNumber>\n      <!--Optional:-->\n      <GRN>string</GRN>\n      <!--Optional:-->\n      <accessCode>stri</accessCode>\n      <!--Optional:-->\n      <amountToBeCovered>1000.000000000000</amountToBeCovered>\n      <!--Optional:-->\n      <currency>token</currency>\n    </GuaranteeReference>")
    }
    xmlValue.append("</Guarantee>\n")
  }

  xmlValue.append("  <Consignment>\n    <!--Optional:-->\n    <countryOfDispatch>st</countryOfDispatch>\n    <!--Optional:-->\n    <countryOfDestination>token</countryOfDestination>\n    <!--Optional:-->\n    <containerIndicator>1</containerIndicator>\n    <!--Optional:-->\n    <inlandModeOfTransport>token</inlandModeOfTransport>\n    <!--Optional:-->\n    <modeOfTransportAtTheBorder>token</modeOfTransportAtTheBorder>\n    <grossMass>1000.000000000000</grossMass>\n    <!--Optional:-->\n    <referenceNumberUCR>string</referenceNumberUCR>\n    <!--Optional:-->\n    <Carrier>\n      <identificationNumber>string</identificationNumber>\n      <!--Optional:-->\n      <ContactPerson>\n        <name>string</name>\n        <phoneNumber>token</phoneNumber>\n        <!--Optional:-->\n        <eMailAddress>string</eMailAddress>\n      </ContactPerson>\n    </Carrier>\n    <!--Optional:-->\n    <Consignor>\n      <!--Optional:-->\n      <identificationNumber>string</identificationNumber>\n      <!--Optional:-->\n      <name>string</name>\n      <!--Optional:-->\n      <Address>\n        <streetAndNumber>string</streetAndNumber>\n        <!--Optional:-->\n        <postcode>string</postcode>\n        <city>string</city>\n        <country>st</country>\n      </Address>\n      <!--Optional:-->\n      <ContactPerson>\n        <name>string</name>\n        <phoneNumber>token</phoneNumber>\n        <!--Optional:-->\n        <eMailAddress>string</eMailAddress>\n      </ContactPerson>\n    </Consignor>\n    <!--Optional:-->\n    <Consignee>\n      <!--Optional:-->\n      <identificationNumber>string</identificationNumber>\n      <!--Optional:-->\n      <name>string</name>\n      <!--Optional:-->\n      <Address>\n        <streetAndNumber>string</streetAndNumber>\n        <!--Optional:-->\n        <postcode>string</postcode>\n        <city>string</city>\n        <country>st</country>\n      </Address>\n    </Consignee>")
  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <AdditionalSupplyChainActor>\n      <sequenceNumber>token</sequenceNumber>\n      <role>token</role>\n      <identificationNumber>string</identificationNumber>\n    </AdditionalSupplyChainActor>")
  }

  xmlValue.append("\n<!--0 to 9999 repetitions:-->\n")
  // TransportEquipment --- done in separate file
  for (i <- 0 to 0) {
    xmlValue.append("    <TransportEquipment>\n      <sequenceNumber>token</sequenceNumber>\n      <!--Optional:-->\n      <containerIdentificationNumber>string</containerIdentificationNumber>\n      <numberOfSeals>100</numberOfSeals>")
    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <Seal>\n        <sequenceNumber>token</sequenceNumber>\n        <identifier>string</identifier>\n      </Seal>")
    }
    xmlValue.append("\n<!--0 to 9999 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <GoodsReference>\n        <sequenceNumber>token</sequenceNumber>\n        <declarationGoodsItemNumber>100</declarationGoodsItemNumber>\n      </GoodsReference>")
    }
    xmlValue.append("</TransportEquipment>")
  }

  xmlValue.append("    <!--Optional:-->\n    <LocationOfGoods>\n      <typeOfLocation>token</typeOfLocation>\n      <qualifierOfIdentification>token</qualifierOfIdentification>\n      <!--Optional:-->\n      <authorisationNumber>string</authorisationNumber>\n      <!--Optional:-->\n      <additionalIdentifier>stri</additionalIdentifier>\n      <!--Optional:-->\n      <UNLocode>token</UNLocode>\n      <!--Optional:-->\n      <CustomsOffice>\n        <referenceNumber>stringst</referenceNumber>\n      </CustomsOffice>\n      <!--Optional:-->\n      <GNSS>\n        <latitude>string</latitude>\n        <longitude>string</longitude>\n      </GNSS>\n      <!--Optional:-->\n      <EconomicOperator>\n        <identificationNumber>string</identificationNumber>\n      </EconomicOperator>\n      <!--Optional:-->\n      <Address>\n        <streetAndNumber>string</streetAndNumber>\n        <!--Optional:-->\n        <postcode>string</postcode>\n        <city>string</city>\n        <country>st</country>\n      </Address>\n      <!--Optional:-->\n      <PostcodeAddress>\n        <!--Optional:-->\n        <houseNumber>string</houseNumber>\n        <postcode>string</postcode>\n        <country>st</country>\n      </PostcodeAddress>\n      <!--Optional:-->\n      <ContactPerson>\n        <name>string</name>\n        <phoneNumber>token</phoneNumber>\n        <!--Optional:-->\n        <eMailAddress>string</eMailAddress>\n      </ContactPerson>\n    </LocationOfGoods>")
  xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <DepartureTransportMeans>\n      <sequenceNumber>token</sequenceNumber>\n      <!--Optional:-->\n      <typeOfIdentification>token</typeOfIdentification>\n      <!--Optional:-->\n      <identificationNumber>string</identificationNumber>\n      <!--Optional:-->\n      <nationality>st</nationality>\n    </DepartureTransportMeans>")
  }

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <CountryOfRoutingOfConsignment>\n      <sequenceNumber>token</sequenceNumber>\n      <country>st</country>\n    </CountryOfRoutingOfConsignment>")
  }

  xmlValue.append("\n<!--0 to 9 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <ActiveBorderTransportMeans>\n      <sequenceNumber>token</sequenceNumber>\n      <!--Optional:-->\n      <customsOfficeAtBorderReferenceNumber>token</customsOfficeAtBorderReferenceNumber>\n      <!--Optional:-->\n      <typeOfIdentification>token</typeOfIdentification>\n      <!--Optional:-->\n      <identificationNumber>string</identificationNumber>\n      <!--Optional:-->\n      <nationality>st</nationality>\n      <!--Optional:-->\n      <conveyanceReferenceNumber>string</conveyanceReferenceNumber>\n    </ActiveBorderTransportMeans>")
  }

  xmlValue.append("    <!--Optional:-->\n    <PlaceOfLoading>\n      <!--Optional:-->\n      <UNLocode>token</UNLocode>\n      <!--Optional:-->\n      <country>st</country>\n      <!--Optional:-->\n      <location>string</location>\n    </PlaceOfLoading>\n    <!--Optional:-->\n    <PlaceOfUnloading>\n      <!--Optional:-->\n      <UNLocode>token</UNLocode>\n      <!--Optional:-->\n      <country>st</country>\n      <!--Optional:-->\n      <location>string</location>\n    </PlaceOfUnloading>")

  xmlValue.append("\n<!--0 to 9999 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <PreviousDocument>\n      <sequenceNumber>token</sequenceNumber>\n      <type>token</type>\n      <referenceNumber>string</referenceNumber>\n      <!--Optional:-->\n      <complementOfInformation>string</complementOfInformation>\n    </PreviousDocument>")
  }

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <SupportingDocument>\n      <sequenceNumber>token</sequenceNumber>\n      <type>token</type>\n      <referenceNumber>string</referenceNumber>\n      <!--Optional:-->\n      <documentLineItemNumber>100</documentLineItemNumber>\n      <!--Optional:-->\n      <complementOfInformation>string</complementOfInformation>\n    </SupportingDocument>")
  }

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <TransportDocument>\n      <sequenceNumber>token</sequenceNumber>\n      <type>token</type>\n      <referenceNumber>string</referenceNumber>\n    </TransportDocument>")
  }

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <AdditionalReference>\n      <sequenceNumber>token</sequenceNumber>\n      <type>token</type>\n      <!--Optional:-->\n      <referenceNumber>string</referenceNumber>\n    </AdditionalReference>")
  }

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 0 to 0) {
    xmlValue.append("    <AdditionalInformation>\n      <sequenceNumber>token</sequenceNumber>\n      <code>token</code>\n      <!--Optional:-->\n      <text>string</text>\n    </AdditionalInformation>")
  }

  xmlValue.append("    <!--Optional:-->\n    <TransportCharges>\n      <methodOfPayment>s</methodOfPayment>\n    </TransportCharges>")

  xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
  for (i <- 1 to 1999) {
    xmlValue.append("<HouseConsignment>\n      <sequenceNumber>")
    xmlValue.append(i)
    xmlValue.append("</sequenceNumber>\n      <!--Optional:-->\n      <countryOfDispatch>st</countryOfDispatch>\n      <grossMass>1000.000000000000</grossMass>\n      <!--Optional:-->\n      <referenceNumberUCR>string</referenceNumberUCR>\n      <!--Optional:-->\n      <Consignor>\n        <!--Optional:-->\n        <identificationNumber>string</identificationNumber>\n        <!--Optional:-->\n        <name>string</name>\n        <!--Optional:-->\n        <Address>\n          <streetAndNumber>string</streetAndNumber>\n          <!--Optional:-->\n          <postcode>string</postcode>\n          <city>string</city>\n          <country>st</country>\n        </Address>\n        <!--Optional:-->\n        <ContactPerson>\n          <name>string</name>\n          <phoneNumber>token</phoneNumber>\n          <!--Optional:-->\n          <eMailAddress>string</eMailAddress>\n        </ContactPerson>\n      </Consignor>\n      <!--Optional:-->\n      <Consignee>\n        <!--Optional:-->\n        <identificationNumber>string</identificationNumber>\n        <!--Optional:-->\n        <name>string</name>\n        <!--Optional:-->\n        <Address>\n          <streetAndNumber>string</streetAndNumber>\n          <!--Optional:-->\n          <postcode>string</postcode>\n          <city>string</city>\n          <country>st</country>\n        </Address>\n      </Consignee>")

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <AdditionalSupplyChainActor>\n        <sequenceNumber>token</sequenceNumber>\n        <role>token</role>\n        <identificationNumber>string</identificationNumber>\n      </AdditionalSupplyChainActor>")
    }

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <DepartureTransportMeans>\n        <sequenceNumber>token</sequenceNumber>\n        <typeOfIdentification>token</typeOfIdentification>\n        <identificationNumber>string</identificationNumber>\n        <nationality>st</nationality>\n      </DepartureTransportMeans>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <PreviousDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n        <!--Optional:-->\n        <complementOfInformation>string</complementOfInformation>\n      </PreviousDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <SupportingDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n        <!--Optional:-->\n        <documentLineItemNumber>100</documentLineItemNumber>\n        <!--Optional:-->\n        <complementOfInformation>string</complementOfInformation>\n      </SupportingDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <TransportDocument>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <referenceNumber>string</referenceNumber>\n      </TransportDocument>")
    }

    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <AdditionalReference>\n        <sequenceNumber>token</sequenceNumber>\n        <type>token</type>\n        <!--Optional:-->\n        <referenceNumber>string</referenceNumber>\n      </AdditionalReference>")
    }

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <AdditionalInformation>\n        <sequenceNumber>token</sequenceNumber>\n        <code>token</code>\n        <!--Optional:-->\n        <text>string</text>\n      </AdditionalInformation>")
    }

    xmlValue.append("      <!--Optional:-->\n      <TransportCharges>\n        <methodOfPayment>s</methodOfPayment>\n      </TransportCharges>")

    xmlValue.append("\n<!--0 to 999 repetitions:-->\n")
    for (i <- 0 to 0) {
      xmlValue.append("      <ConsignmentItem>\n        <goodsItemNumber>token</goodsItemNumber>\n        <declarationGoodsItemNumber>100</declarationGoodsItemNumber>\n        <!--Optional:-->\n        <declarationType>token</declarationType>\n        <!--Optional:-->\n        <countryOfDispatch>st</countryOfDispatch>\n        <!--Optional:-->\n        <countryOfDestination>token</countryOfDestination>\n        <!--Optional:-->\n        <referenceNumberUCR>string</referenceNumberUCR>\n        <!--Optional:-->\n        <Consignee>\n          <!--Optional:-->\n          <identificationNumber>string</identificationNumber>\n          <!--Optional:-->\n          <name>string</name>\n          <!--Optional:-->\n          <Address>\n            <streetAndNumber>string</streetAndNumber>\n            <!--Optional:-->\n            <postcode>string</postcode>\n            <city>string</city>\n            <country>st</country>\n          </Address>\n        </Consignee>")

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <AdditionalSupplyChainActor>\n          <sequenceNumber>token</sequenceNumber>\n          <role>token</role>\n          <identificationNumber>string</identificationNumber>\n        </AdditionalSupplyChainActor>")
      }

      xmlValue.append("        <Commodity>\n          <descriptionOfGoods>string</descriptionOfGoods>\n          <!--Optional:-->\n          <cusCode>token</cusCode>\n          <!--Optional:-->\n          <CommodityCode>\n            <harmonizedSystemSubHeadingCode>token</harmonizedSystemSubHeadingCode>\n            <!--Optional:-->\n            <combinedNomenclatureCode>st</combinedNomenclatureCode>\n          </CommodityCode>")
      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("<DangerousGoods>\n            <sequenceNumber>token</sequenceNumber>\n            <UNNumber>token</UNNumber>\n          </DangerousGoods>")
      }
      xmlValue.append("<!--Optional:-->\n          <GoodsMeasure>\n            <!--Optional:-->\n            <grossMass>1000.000000000000</grossMass>\n            <!--Optional:-->\n            <netMass>1000.000000000000</netMass>\n            <!--Optional:-->\n            <supplementaryUnits>1000.000000000000</supplementaryUnits>\n          </GoodsMeasure>\n        </Commodity>")

      xmlValue.append("\n<!--1 to 99 repetitions:-->\n")
      for (i <- 1 to 1) {
        xmlValue.append("        <Packaging>\n          <sequenceNumber>token</sequenceNumber>\n          <typeOfPackages>token</typeOfPackages>\n          <!--Optional:-->\n          <numberOfPackages>100</numberOfPackages>\n          <!--Optional:-->\n          <shippingMarks>string</shippingMarks>\n        </Packaging>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <PreviousDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n          <!--Optional:-->\n          <goodsItemNumber>100</goodsItemNumber>\n          <!--Optional:-->\n          <typeOfPackages>token</typeOfPackages>\n          <!--Optional:-->\n          <numberOfPackages>100</numberOfPackages>\n          <!--Optional:-->\n          <measurementUnitAndQualifier>token</measurementUnitAndQualifier>\n          <!--Optional:-->\n          <quantity>1000.000000000000</quantity>\n          <!--Optional:-->\n          <complementOfInformation>string</complementOfInformation>\n        </PreviousDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <SupportingDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n          <!--Optional:-->\n          <documentLineItemNumber>100</documentLineItemNumber>\n          <!--Optional:-->\n          <complementOfInformation>string</complementOfInformation>\n        </SupportingDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <TransportDocument>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <referenceNumber>string</referenceNumber>\n        </TransportDocument>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <AdditionalReference>\n          <sequenceNumber>token</sequenceNumber>\n          <type>token</type>\n          <!--Optional:-->\n          <referenceNumber>string</referenceNumber>\n        </AdditionalReference>")
      }

      xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
      for (i <- 0 to 0) {
        xmlValue.append("        <AdditionalInformation>\n          <sequenceNumber>token</sequenceNumber>\n          <code>token</code>\n          <!--Optional:-->\n          <text>string</text>\n        </AdditionalInformation>")
      }

      xmlValue.append("        <!--Optional:-->\n        <TransportCharges>\n          <methodOfPayment>s</methodOfPayment>\n        </TransportCharges>")

      xmlValue.append("\n</ConsignmentItem>")
    }

    xmlValue.append("\n</HouseConsignment>")
  }


  xmlValue.append("\n </Consignment>")

  xmlValue.append("\n</ncts:CC015C>")

  import java.nio.charset.StandardCharsets
  import java.nio.file.{Files, Paths}

  Files.write(Paths.get("ie015HC.xml"), xmlValue.toString().getBytes(StandardCharsets.UTF_8))

}
