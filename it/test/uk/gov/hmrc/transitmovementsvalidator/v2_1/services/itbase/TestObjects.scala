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

package uk.gov.hmrc.transitmovementsvalidator.v2_1.services.itbase

import play.api.libs.json.Json

object TestObjects {

  object CC007C {
    lazy val xmlValid = <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC007C</messageType>
      <correlationIdentifier>token</correlationIdentifier>
      <TransitOperation>
        <MRN>27WF9X1FQ9RCKN0TM3</MRN>
        <arrivalNotificationDateAndTime>2022-07-02T03:11:04</arrivalNotificationDateAndTime>
        <simplifiedProcedure>1</simplifiedProcedure>
        <incidentFlag>1</incidentFlag>
      </TransitOperation>
      <Authorisation>
        <sequenceNumber>123</sequenceNumber>
        <type>3344</type>
        <referenceNumber>token</referenceNumber>
      </Authorisation>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
      <TraderAtDestination>
        <identificationNumber>ezv3Z</identificationNumber>
        <communicationLanguageAtDestination>sa</communicationLanguageAtDestination>
      </TraderAtDestination>
      <Consignment>
        <LocationOfGoods>
          <typeOfLocation>A</typeOfLocation>
          <qualifierOfIdentification>A</qualifierOfIdentification>
          <authorisationNumber>token</authorisationNumber>
          <additionalIdentifier>1234</additionalIdentifier>
          <UNLocode>token</UNLocode>
          <CustomsOffice>
            <referenceNumber>AB234567</referenceNumber>
          </CustomsOffice>

          <EconomicOperator>
            <identificationNumber>ezv3Z</identificationNumber>
          </EconomicOperator>
          <Address>
            <streetAndNumber>token</streetAndNumber>
            <postcode>token</postcode>
            <city>token</city>
            <country>GB</country>
          </Address>
          <PostcodeAddress>
            <houseNumber>token</houseNumber>
            <postcode>token</postcode>
            <country>SA</country>
          </PostcodeAddress>
          <ContactPerson>
            <name>token</name>
            <phoneNumber>token</phoneNumber>
            <eMailAddress>sandeep@gmail.com</eMailAddress>
          </ContactPerson>
        </LocationOfGoods>
        <Incident>
          <sequenceNumber>12345</sequenceNumber>
          <code>1</code>
          <text>token</text>
          <Endorsement>
            <date>2022-07-02</date>
            <authority>token</authority>
            <place>token</place>
            <country>GB</country>
          </Endorsement>
          <Location>
            <qualifierOfIdentification>A</qualifierOfIdentification>
            <UNLocode>token</UNLocode>
            <country>SA</country>

            <Address>
              <streetAndNumber>token</streetAndNumber>
              <postcode>token</postcode>
              <city>token</city>
            </Address>
          </Location>
          <TransportEquipment>
            <sequenceNumber>12345</sequenceNumber>
            <containerIdentificationNumber>ezv3Z</containerIdentificationNumber>
            <numberOfSeals>2345</numberOfSeals>
            <Seal>
              <sequenceNumber>12345</sequenceNumber>
              <identifier>token</identifier>
            </Seal>
            <GoodsReference>
              <sequenceNumber>12345</sequenceNumber>
              <declarationGoodsItemNumber>12</declarationGoodsItemNumber>
            </GoodsReference>
          </TransportEquipment>
          <Transhipment>
            <containerIndicator>0</containerIndicator>
            <TransportMeans>
              <typeOfIdentification>12</typeOfIdentification>
              <identificationNumber>ezv3Z</identificationNumber>
              <nationality>GB</nationality>
            </TransportMeans>
          </Transhipment>
        </Incident>
      </Consignment>
    </ncts:CC007C>

    lazy val xmlInvalid = <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageRecipient>FdOcminxBxSLGm1rRUn0q96S1</messageRecipient>
      <messageType>CC007C</messageType>
    </ncts:CC007C>

    lazy val jsonValid = Json.obj(
      "n1:CC007C" -> Json.obj(
        "preparationDateAndTime" -> "2007-10-26T07:36:28",
        "TransitOperation" -> Json.obj(
          "MRN"                            -> "27WF9X1FQ9RCKN0TM3",
          "arrivalNotificationDateAndTime" -> "2022-07-02T03:11:04",
          "simplifiedProcedure"            -> "1",
          "incidentFlag"                   -> "1"
        ),
        "TraderAtDestination" -> Json.obj(
          "identificationNumber"               -> "ezv3Z",
          "communicationLanguageAtDestination" -> "sa"
        ),
        "messageType"           -> "CC007C",
        "@PhaseID"              -> "NCTS5.0",
        "correlationIdentifier" -> "token",
        "Authorisation" -> Json.arr(
          Json.obj(
            "sequenceNumber"  -> 123,
            "referenceNumber" -> "token",
            "type"            -> "3344"
          )
        ),
        "messageSender"    -> "token",
        "messageRecipient" -> "token",
        "Consignment" -> Json.obj(
          "LocationOfGoods" -> Json.obj(
            "typeOfLocation"            -> "A",
            "qualifierOfIdentification" -> "A",
            "authorisationNumber"       -> "token",
            "additionalIdentifier"      -> "1234",
            "UNLocode"                  -> "token",
            "CustomsOffice" -> Json.obj(
              "referenceNumber" -> "AB123456"
            ),
            "EconomicOperator" -> Json.obj(
              "identificationNumber" -> "ezv3Z"
            ),
            "Address" -> Json.obj(
              "streetAndNumber" -> "token",
              "postcode"        -> "token",
              "city"            -> "token",
              "country"         -> "GB"
            ),
            "PostcodeAddress" -> Json.obj(
              "houseNumber" -> "token",
              "postcode"    -> "token",
              "country"     -> "SA"
            ),
            "ContactPerson" -> Json.obj(
              "name"         -> "token",
              "phoneNumber"  -> "token",
              "eMailAddress" -> "xxxx@gmail.com"
            )
          ),
          "Incident" -> Json.arr(
            Json.obj(
              "sequenceNumber" -> 12345,
              "code"           -> "1",
              "text"           -> "token",
              "Endorsement" -> Json.obj(
                "date"      -> "2022-07-02",
                "authority" -> "token",
                "place"     -> "token",
                "country"   -> "GB"
              ),
              "Location" -> Json.obj(
                "qualifierOfIdentification" -> "A",
                "UNLocode"                  -> "token",
                "country"                   -> "SA",
                "Address" -> Json.obj(
                  "streetAndNumber" -> "token",
                  "postcode"        -> "token",
                  "city"            -> "token"
                )
              ),
              "TransportEquipment" -> Json.arr(
                Json.obj(
                  "sequenceNumber"                -> 12345,
                  "containerIdentificationNumber" -> "ezv3Z",
                  "numberOfSeals"                 -> 2345,
                  "Seal" -> Json.arr(
                    Json.obj(
                      "sequenceNumber" -> 12345,
                      "identifier"     -> "token"
                    )
                  ),
                  "GoodsReference" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"             -> 12345,
                      "declarationGoodsItemNumber" -> 12
                    )
                  )
                )
              ),
              "Transhipment" -> Json.obj(
                "containerIndicator" -> "0",
                "TransportMeans" -> Json.obj(
                  "typeOfIdentification" -> "12",
                  "identificationNumber" -> "ezv3Z",
                  "nationality"          -> "GB"
                )
              )
            )
          )
        ),
        "messageIdentification" -> "token",
        "CustomsOfficeOfDestinationActual" -> Json.obj(
          "referenceNumber" -> "GB123456"
        )
      )
    )

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC007C" ->
          Json.obj(
            "@PhaseID"         -> "NCTS5.0",
            "messageRecipient" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"      -> "CC007C"
          )
      )
  }

  object CC013C {

    lazy val xmlValid = <ncts:CC013C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC013C</messageType>
      <TransitOperation>
        <declarationType>token</declarationType>
        <additionalDeclarationType>A</additionalDeclarationType>
        <security>0</security>
        <reducedDatasetIndicator>1</reducedDatasetIndicator>
        <bindingItinerary>1</bindingItinerary>
        <amendmentTypeFlag>0</amendmentTypeFlag>
      </TransitOperation>
      <!--0 to 9 repetitions:-->
      <Authorisation>
        <sequenceNumber>12345</sequenceNumber>
        <type>tokn</type>
        <referenceNumber>GB444</referenceNumber>
      </Authorisation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <!--0 to 9 repetitions:-->
      <CustomsOfficeOfTransitDeclared>
        <sequenceNumber>12345</sequenceNumber>
        <referenceNumber>AB123456</referenceNumber>
      </CustomsOfficeOfTransitDeclared>
      <!--0 to 9 repetitions:-->
      <CustomsOfficeOfExitForTransitDeclared>
        <sequenceNumber>12345</sequenceNumber>
        <referenceNumber>AB123456</referenceNumber>
      </CustomsOfficeOfExitForTransitDeclared>
      <HolderOfTheTransitProcedure>
      </HolderOfTheTransitProcedure>
      <!--1 to 9 repetitions:-->
      <Guarantee>
        <sequenceNumber>12345</sequenceNumber>
        <!--0 to 99 repetitions:-->
        <GuaranteeReference>
          <sequenceNumber>12345</sequenceNumber>
        </GuaranteeReference>
      </Guarantee>
      <Consignment>
        <grossMass>1000.000000000000</grossMass>
        <!--0 to 99 repetitions:-->
        <AdditionalSupplyChainActor>
          <sequenceNumber>12345</sequenceNumber>
          <role>ABC</role>
          <identificationNumber>GB123</identificationNumber>
        </AdditionalSupplyChainActor>
        <!--0 to 9999 repetitions:-->
        <TransportEquipment>
          <sequenceNumber>12345</sequenceNumber>
          <numberOfSeals>100</numberOfSeals>
          <!--0 to 99 repetitions:-->
          <Seal>
            <sequenceNumber>12345</sequenceNumber>
            <identifier>string</identifier>
          </Seal>
          <!--0 to 9999 repetitions:-->
          <GoodsReference>
            <sequenceNumber>12345</sequenceNumber>
            <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
          </GoodsReference>
        </TransportEquipment>
        <!--0 to 999 repetitions:-->
        <DepartureTransportMeans>
          <sequenceNumber>12345</sequenceNumber>
        </DepartureTransportMeans>
        <!--0 to 99 repetitions:-->
        <CountryOfRoutingOfConsignment>
          <sequenceNumber>12345</sequenceNumber>
          <country>GB</country>
        </CountryOfRoutingOfConsignment>
        <!--0 to 9 repetitions:-->
        <ActiveBorderTransportMeans>
          <sequenceNumber>12345</sequenceNumber>
        </ActiveBorderTransportMeans>
        <!--0 to 9999 repetitions:-->
        <PreviousDocument>
          <sequenceNumber>12345</sequenceNumber>
          <type>ABCD</type>
          <referenceNumber>AB123456</referenceNumber>
        </PreviousDocument>
        <!--0 to 99 repetitions:-->
        <SupportingDocument>
          <sequenceNumber>12345</sequenceNumber>
          <type>ABCD</type>
          <referenceNumber>AB123456</referenceNumber>
        </SupportingDocument>
        <!--0 to 99 repetitions:-->
        <TransportDocument>
          <sequenceNumber>12345</sequenceNumber>
          <type>ABCD</type>
          <referenceNumber>AB123456</referenceNumber>
        </TransportDocument>
        <!--0 to 99 repetitions:-->
        <AdditionalReference>
          <sequenceNumber>12345</sequenceNumber>
          <type>ABCD</type>
        </AdditionalReference>
        <!--0 to 99 repetitions:-->
        <AdditionalInformation>
          <sequenceNumber>12345</sequenceNumber>
          <code>token</code>
        </AdditionalInformation>
        <!--1 to 99 repetitions:-->
        <HouseConsignment>
          <sequenceNumber>12345</sequenceNumber>
          <grossMass>1000.000000000000</grossMass>
          <!--0 to 99 repetitions:-->
          <AdditionalSupplyChainActor>
            <sequenceNumber>12345</sequenceNumber>
            <role>ABC</role>
            <identificationNumber>GB123</identificationNumber>
          </AdditionalSupplyChainActor>
          <!--0 to 999 repetitions:-->
          <DepartureTransportMeans>
            <sequenceNumber>12345</sequenceNumber>
            <typeOfIdentification>01</typeOfIdentification>
            <identificationNumber>GB123</identificationNumber>
            <nationality>GB</nationality>
          </DepartureTransportMeans>
          <!--0 to 99 repetitions:-->
          <PreviousDocument>
            <sequenceNumber>12345</sequenceNumber>
            <type>ABCD</type>
            <referenceNumber>AB123456</referenceNumber>
          </PreviousDocument>
          <!--0 to 99 repetitions:-->
          <SupportingDocument>
            <sequenceNumber>12345</sequenceNumber>
            <type>ABCD</type>
            <referenceNumber>AB123456</referenceNumber>
          </SupportingDocument>
          <!--0 to 99 repetitions:-->
          <TransportDocument>
            <sequenceNumber>12345</sequenceNumber>
            <type>ABCD</type>
            <referenceNumber>AB123456</referenceNumber>
          </TransportDocument>
          <!--0 to 99 repetitions:-->
          <AdditionalReference>
            <sequenceNumber>12345</sequenceNumber>
            <type>ABCD</type>
          </AdditionalReference>
          <!--0 to 99 repetitions:-->
          <AdditionalInformation>
            <sequenceNumber>12345</sequenceNumber>
            <code>token</code>
          </AdditionalInformation>
          <!--1 to 999 repetitions:-->
          <ConsignmentItem>
            <goodsItemNumber>12345</goodsItemNumber>
            <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
            <!--0 to 99 repetitions:-->
            <AdditionalSupplyChainActor>
              <sequenceNumber>12345</sequenceNumber>
              <role>ABC</role>
              <identificationNumber>GB123</identificationNumber>
            </AdditionalSupplyChainActor>
            <Commodity>
              <descriptionOfGoods>string</descriptionOfGoods>
              <!--0 to 99 repetitions:-->
              <DangerousGoods>
                <sequenceNumber>12345</sequenceNumber>
                <UNNumber>1234</UNNumber>
              </DangerousGoods>
            </Commodity>
            <!--1 to 99 repetitions:-->
            <Packaging>
              <sequenceNumber>12345</sequenceNumber>
              <typeOfPackages>11</typeOfPackages>
            </Packaging>
            <!--0 to 99 repetitions:-->
            <PreviousDocument>
              <sequenceNumber>12345</sequenceNumber>
              <type>ABCD</type>
              <referenceNumber>AB123456</referenceNumber>
            </PreviousDocument>
            <!--0 to 99 repetitions:-->
            <SupportingDocument>
              <sequenceNumber>12345</sequenceNumber>
              <type>ABCD</type>
              <referenceNumber>AB123456</referenceNumber>
            </SupportingDocument>
            <!--0 to 99 repetitions:-->
            <TransportDocument>
              <sequenceNumber>12345</sequenceNumber>
              <type>ABCD</type>
              <referenceNumber>AB123456</referenceNumber>
            </TransportDocument>
            <!--0 to 99 repetitions:-->
            <AdditionalReference>
              <sequenceNumber>12345</sequenceNumber>
              <type>ABCD</type>
            </AdditionalReference>
            <!--0 to 99 repetitions:-->
            <AdditionalInformation>
              <sequenceNumber>12345</sequenceNumber>
              <code>token</code>
            </AdditionalInformation>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC013C>

    lazy val xmlInvalid = <ncts:CC013C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>FdOcminxBxSLGm1rRUn0q96S1</messageSender>
      <messageType>CC013C</messageType>
    </ncts:CC013C>

    lazy val jsonValid = Json.obj(
      "n1:CC013C" -> Json.obj(
        "preparationDateAndTime" -> "2007-10-26T07:36:28",
        "TransitOperation" -> Json.obj(
          "LRN"                               -> "string",
          "MRN"                               -> "29GMFMHYWWCFCWRVJ3",
          "declarationType"                   -> "token",
          "additionalDeclarationType"         -> "a",
          "TIRCarnetNumber"                   -> "123456",
          "presentationOfTheGoodsDateAndTime" -> "2014-06-09T16:15:04",
          "security"                          -> "0",
          "reducedDatasetIndicator"           -> "1",
          "specificCircumstanceIndicator"     -> "abc",
          "communicationLanguageAtDeparture"  -> "GB",
          "bindingItinerary"                  -> "1",
          "amendmentTypeFlag"                 -> "0",
          "limitDate"                         -> "2017-05-15"
        ),
        "CustomsOfficeOfDeparture" -> Json.obj(
          "referenceNumber" -> "GB123456"
        ),
        "Consignment" -> Json.obj(
          "countryOfDispatch"          -> "GB",
          "countryOfDestination"       -> "XI",
          "containerIndicator"         -> "1",
          "inlandModeOfTransport"      -> "0",
          "modeOfTransportAtTheBorder" -> "1",
          "grossMass"                  -> 1000,
          "referenceNumberUCR"         -> "string",
          "Carrier" -> Json.obj(
            "identificationNumber" -> "AZ!-",
            "ContactPerson" -> Json.obj(
              "name"         -> "string",
              "phoneNumber"  -> "token",
              "eMailAddress" -> "string"
            )
          ),
          "Consignor" -> Json.obj(
            "identificationNumber" -> "string",
            "name"                 -> "string",
            "Address" -> Json.obj(
              "streetAndNumber" -> "string",
              "postcode"        -> "string",
              "city"            -> "string",
              "country"         -> "GB"
            ),
            "ContactPerson" -> Json.obj(
              "name"         -> "string",
              "phoneNumber"  -> "token",
              "eMailAddress" -> "string"
            )
          ),
          "Consignee" -> Json.obj(
            "identificationNumber" -> "string",
            "name"                 -> "string",
            "Address" -> Json.obj(
              "streetAndNumber" -> "string",
              "postcode"        -> "string",
              "city"            -> "string",
              "country"         -> "GB"
            )
          ),
          "AdditionalSupplyChainActor" -> Json.arr(
            Json.obj(
              "sequenceNumber"       -> 123,
              "role"                 -> "abc",
              "identificationNumber" -> "AB!"
            )
          ),
          "TransportEquipment" -> Json.arr(
            Json.obj(
              "sequenceNumber"                -> 123,
              "containerIdentificationNumber" -> "string",
              "numberOfSeals"                 -> 100,
              "Seal" -> Json.arr(
                Json.obj(
                  "sequenceNumber" -> 123,
                  "identifier"     -> "string"
                )
              ),
              "GoodsReference" -> Json.arr(
                Json.obj(
                  "sequenceNumber"             -> 123,
                  "declarationGoodsItemNumber" -> 100
                )
              )
            )
          ),
          "LocationOfGoods" -> Json.obj(
            "typeOfLocation"            -> "a",
            "qualifierOfIdentification" -> "a",
            "authorisationNumber"       -> "string",
            "additionalIdentifier"      -> "stri",
            "UNLocode"                  -> "token",
            "CustomsOffice" -> Json.obj(
              "referenceNumber" -> "GB123456"
            ),
            "GNSS" -> Json.obj(
              "latitude"  -> "+1.00000",
              "longitude" -> "+1.00000"
            ),
            "EconomicOperator" -> Json.obj(
              "identificationNumber" -> "string"
            ),
            "Address" -> Json.obj(
              "streetAndNumber" -> "string",
              "postcode"        -> "string",
              "city"            -> "string",
              "country"         -> "GB"
            ),
            "PostcodeAddress" -> Json.obj(
              "houseNumber" -> "string",
              "postcode"    -> "string",
              "country"     -> "GB"
            ),
            "ContactPerson" -> Json.obj(
              "name"         -> "string",
              "phoneNumber"  -> "token",
              "eMailAddress" -> "string"
            )
          ),
          "DepartureTransportMeans" -> Json.arr(
            Json.obj(
              "sequenceNumber"       -> 123,
              "typeOfIdentification" -> "12",
              "identificationNumber" -> "string",
              "nationality"          -> "XI"
            )
          ),
          "CountryOfRoutingOfConsignment" -> Json.arr(
            Json.obj(
              "sequenceNumber" -> 123,
              "country"        -> "GB"
            )
          ),
          "ActiveBorderTransportMeans" -> Json.arr(
            Json.obj(
              "sequenceNumber"                       -> 123,
              "customsOfficeAtBorderReferenceNumber" -> "abcdabcd",
              "typeOfIdentification"                 -> "12",
              "identificationNumber"                 -> "string",
              "nationality"                          -> "GB",
              "conveyanceReferenceNumber"            -> "string"
            )
          ),
          "PlaceOfLoading" -> Json.obj(
            "UNLocode" -> "token",
            "country"  -> "GB",
            "location" -> "string"
          ),
          "PlaceOfUnloading" -> Json.obj(
            "UNLocode" -> "token",
            "country"  -> "GB",
            "location" -> "string"
          ),
          "PreviousDocument" -> Json.arr(
            Json.obj(
              "sequenceNumber"          -> 123,
              "type"                    -> "abcd",
              "referenceNumber"         -> "string",
              "complementOfInformation" -> "string"
            )
          ),
          "SupportingDocument" -> Json.arr(
            Json.obj(
              "sequenceNumber"          -> 123,
              "type"                    -> "abcd",
              "referenceNumber"         -> "string",
              "documentLineItemNumber"  -> 100,
              "complementOfInformation" -> "string"
            )
          ),
          "TransportDocument" -> Json.arr(
            Json.obj(
              "sequenceNumber"  -> 123,
              "type"            -> "abcd",
              "referenceNumber" -> "string"
            )
          ),
          "AdditionalReference" -> Json.arr(
            Json.obj(
              "sequenceNumber"  -> 123,
              "type"            -> "abcd",
              "referenceNumber" -> "string"
            )
          ),
          "AdditionalInformation" -> Json.arr(
            Json.obj(
              "sequenceNumber" -> 123,
              "code"           -> "token",
              "text"           -> "string"
            )
          ),
          "TransportCharges" -> Json.obj(
            "methodOfPayment" -> "A"
          ),
          "HouseConsignment" -> Json.arr(
            Json.obj(
              "sequenceNumber"     -> 123,
              "countryOfDispatch"  -> "st",
              "grossMass"          -> 1000,
              "referenceNumberUCR" -> "string",
              "Consignor" -> Json.obj(
                "identificationNumber" -> "string",
                "name"                 -> "string",
                "Address" -> Json.obj(
                  "streetAndNumber" -> "string",
                  "postcode"        -> "string",
                  "city"            -> "string",
                  "country"         -> "GB"
                ),
                "ContactPerson" -> Json.obj(
                  "name"         -> "string",
                  "phoneNumber"  -> "token",
                  "eMailAddress" -> "string"
                )
              ),
              "Consignee" -> Json.obj(
                "identificationNumber" -> "string",
                "name"                 -> "string",
                "Address" -> Json.obj(
                  "streetAndNumber" -> "string",
                  "postcode"        -> "string",
                  "city"            -> "string",
                  "country"         -> "GB"
                )
              ),
              "AdditionalSupplyChainActor" -> Json.arr(
                Json.obj(
                  "sequenceNumber"       -> 123,
                  "role"                 -> "abc",
                  "identificationNumber" -> "AV!---"
                )
              ),
              "DepartureTransportMeans" -> Json.arr(
                Json.obj(
                  "sequenceNumber"       -> 123,
                  "typeOfIdentification" -> "12",
                  "identificationNumber" -> "string",
                  "nationality"          -> "XI"
                )
              ),
              "PreviousDocument" -> Json.arr(
                Json.obj(
                  "sequenceNumber"          -> 123,
                  "type"                    -> "abcd",
                  "referenceNumber"         -> "string",
                  "complementOfInformation" -> "string"
                )
              ),
              "SupportingDocument" -> Json.arr(
                Json.obj(
                  "sequenceNumber"          -> 123,
                  "type"                    -> "abcd",
                  "referenceNumber"         -> "string",
                  "documentLineItemNumber"  -> 100,
                  "complementOfInformation" -> "string"
                )
              ),
              "TransportDocument" -> Json.arr(
                Json.obj(
                  "sequenceNumber"  -> 123,
                  "type"            -> "abcd",
                  "referenceNumber" -> "string"
                )
              ),
              "AdditionalReference" -> Json.arr(
                Json.obj(
                  "sequenceNumber"  -> 123,
                  "type"            -> "abcd",
                  "referenceNumber" -> "string"
                )
              ),
              "AdditionalInformation" -> Json.arr(
                Json.obj(
                  "sequenceNumber" -> 123,
                  "code"           -> "token",
                  "text"           -> "string"
                )
              ),
              "TransportCharges" -> Json.obj(
                "methodOfPayment" -> "A"
              ),
              "ConsignmentItem" -> Json.arr(
                Json.obj(
                  "goodsItemNumber"            -> 123,
                  "declarationGoodsItemNumber" -> 100,
                  "declarationType"            -> "token",
                  "countryOfDispatch"          -> "GB",
                  "countryOfDestination"       -> "XI",
                  "referenceNumberUCR"         -> "string",
                  "Consignee" -> Json.obj(
                    "identificationNumber" -> "string",
                    "name"                 -> "string",
                    "Address" -> Json.obj(
                      "streetAndNumber" -> "string",
                      "postcode"        -> "string",
                      "city"            -> "string",
                      "country"         -> "GB"
                    )
                  ),
                  "AdditionalSupplyChainActor" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"       -> 123,
                      "role"                 -> "abc",
                      "identificationNumber" -> "AB!--!"
                    )
                  ),
                  "Commodity" -> Json.obj(
                    "descriptionOfGoods" -> "string",
                    "cusCode"            -> "abcdefghi",
                    "CommodityCode" -> Json.obj(
                      "harmonizedSystemSubHeadingCode" -> "tokent",
                      "combinedNomenclatureCode"       -> "st"
                    ),
                    "DangerousGoods" -> Json.arr(
                      Json.obj(
                        "sequenceNumber" -> 123,
                        "UNNumber"       -> "abcd"
                      )
                    ),
                    "GoodsMeasure" -> Json.obj(
                      "grossMass"          -> 1000,
                      "netMass"            -> 1000,
                      "supplementaryUnits" -> 1000
                    )
                  ),
                  "Packaging" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"   -> 123,
                      "typeOfPackages"   -> "ab",
                      "numberOfPackages" -> 100,
                      "shippingMarks"    -> "string"
                    )
                  ),
                  "PreviousDocument" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"              -> 123,
                      "type"                        -> "abcd",
                      "referenceNumber"             -> "string",
                      "goodsItemNumber"             -> 100,
                      "typeOfPackages"              -> "ab",
                      "numberOfPackages"            -> 100,
                      "measurementUnitAndQualifier" -> "a",
                      "quantity"                    -> 1000,
                      "complementOfInformation"     -> "string"
                    )
                  ),
                  "SupportingDocument" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"          -> 123,
                      "type"                    -> "abcd",
                      "referenceNumber"         -> "string",
                      "documentLineItemNumber"  -> 100,
                      "complementOfInformation" -> "string"
                    )
                  ),
                  "TransportDocument" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"  -> 123,
                      "type"            -> "abcd",
                      "referenceNumber" -> "string"
                    )
                  ),
                  "AdditionalReference" -> Json.arr(
                    Json.obj(
                      "sequenceNumber"  -> 123,
                      "type"            -> "abcd",
                      "referenceNumber" -> "string"
                    )
                  ),
                  "AdditionalInformation" -> Json.arr(
                    Json.obj(
                      "sequenceNumber" -> 123,
                      "code"           -> "token",
                      "text"           -> "string"
                    )
                  ),
                  "TransportCharges" -> Json.obj(
                    "methodOfPayment" -> "A"
                  )
                )
              )
            )
          )
        ),
        "CustomsOfficeOfExitForTransitDeclared" -> Json.arr(
          Json.obj(
            "sequenceNumber"  -> 123,
            "referenceNumber" -> "AB123456"
          )
        ),
        "CustomsOfficeOfTransitDeclared" -> Json.arr(
          Json.obj(
            "sequenceNumber"              -> 123,
            "referenceNumber"             -> "AB123456",
            "arrivalDateAndTimeEstimated" -> "2013-12-21T11:32:42"
          )
        ),
        "CustomsOfficeOfDestinationDeclared" -> Json.obj(
          "referenceNumber" -> "GB123456"
        ),
        "messageType"           -> "CC013C",
        "@PhaseID"              -> "NCTS5.0",
        "correlationIdentifier" -> "token",
        "Authorisation" -> Json.arr(
          Json.obj(
            "sequenceNumber"  -> 123,
            "type"            -> "0",
            "referenceNumber" -> "string"
          )
        ),
        "messageRecipient" -> "token",
        "messageSender"    -> "token",
        "HolderOfTheTransitProcedure" -> Json.obj(
          "identificationNumber"          -> "string",
          "TIRHolderIdentificationNumber" -> "string",
          "name"                          -> "string",
          "Address" -> Json.obj(
            "streetAndNumber" -> "string",
            "postcode"        -> "string",
            "city"            -> "string",
            "country"         -> "GB"
          ),
          "ContactPerson" -> Json.obj(
            "name"         -> "string",
            "phoneNumber"  -> "token",
            "eMailAddress" -> "string"
          )
        ),
        "Representative" -> Json.obj(
          "identificationNumber" -> "string",
          "status"               -> "0",
          "ContactPerson" -> Json.obj(
            "name"         -> "string",
            "phoneNumber"  -> "token",
            "eMailAddress" -> "string"
          )
        ),
        "messageIdentification" -> "token",
        "Guarantee" -> Json.arr(
          Json.obj(
            "sequenceNumber"          -> 123,
            "guaranteeType"           -> "B",
            "otherGuaranteeReference" -> "GBP",
            "GuaranteeReference" -> Json.arr(
              Json.obj(
                "sequenceNumber"    -> 123,
                "GRN"               -> "ABC",
                "accessCode"        -> "stri",
                "amountToBeCovered" -> 1000,
                "currency"          -> "GBP"
              )
            )
          )
        )
      )
    )

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC013C" ->
          Json.obj(
            "@PhaseID"         -> "NCTS5.0",
            "messageRecipient" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"      -> "CC013C"
          )
      )

  }

  object CC014C {

    lazy val xmlValid = <ncts:CC014C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC014C</messageType>
      <TransitOperation>
      </TransitOperation>
      <Invalidation>
        <initiatedByCustoms>1</initiatedByCustoms>
      </Invalidation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDeparture>
      <HolderOfTheTransitProcedure>
      </HolderOfTheTransitProcedure>
    </ncts:CC014C>

    lazy val xmlInvalid = <ncts:CC014C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>FdOcminxBxSLGm1rRUn0q96S1</messageSender>
      <messageType>CC014C</messageType>
    </ncts:CC014C>

    lazy val jsonValid = Json.obj(
      "n1:CC014C" -> Json.obj(
        "preparationDateAndTime" -> "2007-10-26T07:36:28",
        "TransitOperation" -> Json.obj(
          "LRN" -> "string",
          "MRN" -> "29GMFMHYWWCFCWRVJ3"
        ),
        "CustomsOfficeOfDeparture" -> Json.obj(
          "referenceNumber" -> "GB123456"
        ),
        "messageType"           -> "CC014C",
        "@PhaseID"              -> "NCTS5.0",
        "correlationIdentifier" -> "token",
        "messageRecipient"      -> "token",
        "messageSender"         -> "token",
        "HolderOfTheTransitProcedure" -> Json.obj(
          "identificationNumber"          -> "string",
          "TIRHolderIdentificationNumber" -> "string",
          "name"                          -> "string",
          "Address" -> Json.obj(
            "streetAndNumber" -> "string",
            "postcode"        -> "string",
            "city"            -> "string",
            "country"         -> "GB"
          ),
          "ContactPerson" -> Json.obj(
            "name"         -> "string",
            "phoneNumber"  -> "token",
            "eMailAddress" -> "string"
          )
        ),
        "Invalidation" -> Json.obj(
          "requestDateAndTime"  -> "2014-06-09T16:15:04",
          "decisionDateAndTime" -> "2008-11-15T16:52:58",
          "decision"            -> "0",
          "initiatedByCustoms"  -> "1",
          "justification"       -> "string"
        ),
        "messageIdentification" -> "token"
      )
    )

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC014C" ->
          Json.obj(
            "@PhaseID"         -> "NCTS5.0",
            "messageRecipient" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"      -> "CC014C"
          )
      )
  }

  object CC015C {

    lazy val xmlInvalid = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>FdOcminxBxSLGm1rRUn0q96S1</messageSender>
      <messageType>CC015C</messageType>
    </ncts:CC015C>

    lazy val xmlValid = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>XgklxBxSLRtytuiop23op09q2</messageSender>
      <messageRecipient>FdOcminxBxSLGm1rRUn0q96S1</messageRecipient>
      <preparationDateAndTime>2022-01-22T07:43:36</preparationDateAndTime>
      <messageIdentification>6Onxa3En</messageIdentification>
      <messageType>CC015C</messageType>
      <TransitOperation>
        <LRN>qvRcL</LRN>
        <declarationType>Pbg</declarationType>
        <additionalDeclarationType>O</additionalDeclarationType>
        <security>8</security>
        <reducedDatasetIndicator>1</reducedDatasetIndicator>
        <bindingItinerary>0</bindingItinerary>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GBZ20442</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>ZQZ20442</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <HolderOfTheTransitProcedure>
        <identificationNumber>SFzsisksA</identificationNumber>
      </HolderOfTheTransitProcedure>
      <Guarantee>
        <sequenceNumber>48711</sequenceNumber>
        <guaranteeType>1</guaranteeType>
        <otherGuaranteeReference>1qJMA6MbhnnrOJJjHBHX</otherGuaranteeReference>
      </Guarantee>
      <Consignment>
        <grossMass>6430669292.48125</grossMass>
        <HouseConsignment>
          <sequenceNumber>48711</sequenceNumber>
          <grossMass>6430669292.48125</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>18914</goodsItemNumber>
            <declarationGoodsItemNumber>1458</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>ZMyM5HTSTnLqT5FT9aHXwScqXKC1VitlWeO5gs91cVXBXOB8xBdXG5aGhG9VFjjDGiraIETFfbQWeA7VUokO7ngDOrKZ23ccKKMA6C3GpXciUTt9nS2pzCFFFeg4BXdkIe</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>48711</sequenceNumber>
              <typeOfPackages>Oi</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC015C>

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC015C" ->
          Json.obj(
            "@PhaseID"      -> "NCTS5.0",
            "messageSender" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"   -> "CC015C"
          )
      )

    lazy val jsonValid =
      Json.obj(
        "n1:CC015C" ->
          Json.obj(
            "@PhaseID"               -> "NCTS5.0",
            "messageSender"          -> "XgklxBxSLRtytuiop23op09q2",
            "messageRecipient"       -> "FdOcminxBxSLGm1rRUn0q96S1",
            "preparationDateAndTime" -> "2022-01-22T07:43:36",
            "messageIdentification"  -> "6Onxa3En",
            "messageType"            -> "CC015C",
            "TransitOperation" -> Json.obj(
              "LRN"                       -> "qvRcL",
              "declarationType"           -> "Pbg",
              "additionalDeclarationType" -> "O",
              "security"                  -> "8",
              "reducedDatasetIndicator"   -> "1",
              "bindingItinerary"          -> "0"
            ),
            "CustomsOfficeOfDeparture" -> Json.obj(
              "referenceNumber" -> "GBZ20442"
            ),
            "CustomsOfficeOfDestinationDeclared" -> Json.obj(
              "referenceNumber" -> "ZQZ20442"
            ),
            "HolderOfTheTransitProcedure" -> Json.obj(
              "identificationNumber" -> "SFzsisksA"
            ),
            "Guarantee" -> Json.arr(
              Json.obj(
                "sequenceNumber"          -> 48711,
                "guaranteeType"           -> "1",
                "otherGuaranteeReference" -> "1qJMA6MbhnnrOJJjHBHX",
                "GuaranteeReference"      -> Json.arr()
              )
            ),
            "Consignment" -> Json.obj(
              "grossMass" -> 6430669292.48125,
              "HouseConsignment" -> Json.arr(
                Json.obj(
                  "sequenceNumber"             -> 48711,
                  "grossMass"                  -> 6430669292.48125,
                  "AdditionalSupplyChainActor" -> Json.arr(),
                  "DepartureTransportMeans"    -> Json.arr(),
                  "PreviousDocument"           -> Json.arr(),
                  "SupportingDocument"         -> Json.arr(),
                  "TransportDocument"          -> Json.arr(),
                  "AdditionalReference"        -> Json.arr(),
                  "AdditionalInformation"      -> Json.arr(),
                  "ConsignmentItem" -> Json.arr(
                    Json.obj(
                      "goodsItemNumber"            -> 18914,
                      "declarationGoodsItemNumber" -> 1458,
                      "AdditionalSupplyChainActor" -> Json.arr(),
                      "Commodity" -> Json.obj(
                        "descriptionOfGoods" -> "ZMyM5HTSTnLqT5FT9aHXwScqXKC1VitlWeO5gs91cVXBXOB8xBdXG5aGhG9VFjjDGiraIETFfbQWeA7VUokO7ngDOrKZ23ccKKMA6C3GpXciUTt9nS2pzCFFFeg4BXdkIe",
                        "DangerousGoods"     -> Json.arr()
                      ),
                      "Packaging" -> Json.arr(
                        Json.obj(
                          "sequenceNumber" -> 48711,
                          "typeOfPackages" -> "Oi"
                        )
                      ),
                      "PreviousDocument"      -> Json.arr(),
                      "SupportingDocument"    -> Json.arr(),
                      "TransportDocument"     -> Json.arr(),
                      "AdditionalReference"   -> Json.arr(),
                      "AdditionalInformation" -> Json.arr()
                    )
                  )
                )
              )
            )
          )
      )

  }

  object CC044C {

    lazy val xmlValid = <ncts:CC044C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC044C</messageType>
      <TransitOperation>
        <MRN>27WF9X1FQ9RCKN0TM3</MRN>
      </TransitOperation>
      <CustomsOfficeOfDestinationActual>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDestinationActual>
      <TraderAtDestination>
        <identificationNumber>ezv3Z</identificationNumber>
      </TraderAtDestination>
      <UnloadingRemark>
        <conform>0</conform>
        <unloadingCompletion>0</unloadingCompletion>
        <unloadingDate>2018-11-01</unloadingDate>
      </UnloadingRemark>
    </ncts:CC044C>

    lazy val xmlInvalid = <ncts:CC044C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageRecipient>FdOcminxBxSLGm1rRUn0q96S1</messageRecipient>
      <messageType>CC044C</messageType>
    </ncts:CC044C>

    lazy val jsonValid = Json.obj(
      "n1:CC044C" -> Json.obj(
        "preparationDateAndTime" -> "2007-10-26T07:36:28",
        "TransitOperation" -> Json.obj(
          "MRN" -> "27WF9X1FQ9RCKN0TM3"
        ),
        "TraderAtDestination" -> Json.obj(
          "identificationNumber" -> "ezv3Z"
        ),
        "UnloadingRemark" -> Json.obj(
          "conform"             -> "0",
          "unloadingCompletion" -> "0",
          "unloadingDate"       -> "2018-11-01"
        ),
        "messageType"           -> "CC044C",
        "@PhaseID"              -> "NCTS5.0",
        "messageRecipient"      -> "token",
        "messageSender"         -> "token",
        "messageIdentification" -> "token",
        "CustomsOfficeOfDestinationActual" -> Json.obj(
          "referenceNumber" -> "GB123456"
        )
      )
    )

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC044C" ->
          Json.obj(
            "@PhaseID"      -> "NCTS5.0",
            "messageSender" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"   -> "CC044C"
          )
      )
  }

  object CC170C {

    lazy val xmlValid = <ncts:CC170C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>token</messageSender>
      <messageRecipient>token</messageRecipient>
      <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
      <messageIdentification>token</messageIdentification>
      <messageType>CC170C</messageType>
      <TransitOperation>
        <LRN>string</LRN>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>GB123456</referenceNumber>
      </CustomsOfficeOfDeparture>
      <HolderOfTheTransitProcedure>
      </HolderOfTheTransitProcedure>
      <Consignment>
        <!--0 to 9999 repetitions:-->
        <TransportEquipment>
          <sequenceNumber>12345</sequenceNumber>
          <numberOfSeals>100</numberOfSeals>
          <!--0 to 99 repetitions:-->
          <Seal>
            <sequenceNumber>12345</sequenceNumber>
            <identifier>string</identifier>
          </Seal>
          <!--0 to 9999 repetitions:-->
          <GoodsReference>
            <sequenceNumber>12345</sequenceNumber>
            <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
          </GoodsReference>
        </TransportEquipment>
        <LocationOfGoods>
          <typeOfLocation>A</typeOfLocation>
          <qualifierOfIdentification>B</qualifierOfIdentification>
        </LocationOfGoods>
        <!--0 to 999 repetitions:-->
        <DepartureTransportMeans>
          <sequenceNumber>12345</sequenceNumber>
          <typeOfIdentification>00</typeOfIdentification>
          <identificationNumber>string</identificationNumber>
          <nationality>GB</nationality>
        </DepartureTransportMeans>
        <!--0 to 9 repetitions:-->
        <ActiveBorderTransportMeans>
          <sequenceNumber>12345</sequenceNumber>
          <customsOfficeAtBorderReferenceNumber>12345678</customsOfficeAtBorderReferenceNumber>
          <typeOfIdentification>00</typeOfIdentification>
          <identificationNumber>string</identificationNumber>
          <nationality>GB</nationality>
        </ActiveBorderTransportMeans>
        <!--1 to 99 repetitions:-->
        <HouseConsignment>
          <sequenceNumber>12345</sequenceNumber>
          <!--0 to 999 repetitions:-->
          <DepartureTransportMeans>
            <sequenceNumber>12345</sequenceNumber>
            <typeOfIdentification>00</typeOfIdentification>
            <identificationNumber>string</identificationNumber>
            <nationality>GB</nationality>
          </DepartureTransportMeans>
        </HouseConsignment>
      </Consignment>
    </ncts:CC170C>

    lazy val xmlInvalid = <ncts:CC170C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageRecipient>FdOcminxBxSLGm1rRUn0q96S1</messageRecipient>
      <messageType>CC170C</messageType>
    </ncts:CC170C>

    lazy val jsonValid = Json.obj(
      "n1:CC170C" -> Json.obj(
        "preparationDateAndTime" -> "2007-10-26T07:36:28",
        "TransitOperation" -> Json.obj(
          "LRN"       -> "string",
          "limitDate" -> "2014-06-09"
        ),
        "CustomsOfficeOfDeparture" -> Json.obj(
          "referenceNumber" -> "GB123456"
        ),
        "messageType"           -> "CC170C",
        "@PhaseID"              -> "NCTS5.0",
        "correlationIdentifier" -> "token",
        "messageRecipient"      -> "token",
        "messageSender"         -> "token",
        "HolderOfTheTransitProcedure" -> Json.obj(
          "identificationNumber"          -> "string",
          "TIRHolderIdentificationNumber" -> "string",
          "name"                          -> "string",
          "Address" -> Json.obj(
            "streetAndNumber" -> "string",
            "postcode"        -> "string",
            "city"            -> "string",
            "country"         -> "GB"
          )
        ),
        "Consignment" -> Json.obj(
          "containerIndicator"         -> "1",
          "inlandModeOfTransport"      -> "0",
          "modeOfTransportAtTheBorder" -> "0",
          "TransportEquipment" -> Json.arr(
            Json.obj(
              "sequenceNumber"                -> 1234,
              "containerIdentificationNumber" -> "string",
              "numberOfSeals"                 -> 100,
              "Seal" -> Json.arr(
                Json.obj(
                  "sequenceNumber" -> 1234,
                  "identifier"     -> "string"
                )
              ),
              "GoodsReference" -> Json.arr(
                Json.obj(
                  "sequenceNumber"             -> 1234,
                  "declarationGoodsItemNumber" -> 100
                )
              )
            )
          ),
          "LocationOfGoods" -> Json.obj(
            "typeOfLocation"            -> "b",
            "qualifierOfIdentification" -> "a",
            "authorisationNumber"       -> "string",
            "additionalIdentifier"      -> "stri",
            "UNLocode"                  -> "token",
            "CustomsOffice" -> Json.obj(
              "referenceNumber" -> "AB123456"
            ),
            "GNSS" -> Json.obj(
              "latitude"  -> "+1.00000",
              "longitude" -> "+1.00000"
            ),
            "EconomicOperator" -> Json.obj(
              "identificationNumber" -> "string"
            ),
            "Address" -> Json.obj(
              "streetAndNumber" -> "string",
              "postcode"        -> "string",
              "city"            -> "string",
              "country"         -> "GB"
            ),
            "PostcodeAddress" -> Json.obj(
              "houseNumber" -> "string",
              "postcode"    -> "string",
              "country"     -> "GB"
            ),
            "ContactPerson" -> Json.obj(
              "name"         -> "string",
              "phoneNumber"  -> "token",
              "eMailAddress" -> "string"
            )
          ),
          "DepartureTransportMeans" -> Json.arr(
            Json.obj(
              "sequenceNumber"       -> 1234,
              "typeOfIdentification" -> "12",
              "identificationNumber" -> "string",
              "nationality"          -> "GB"
            )
          ),
          "ActiveBorderTransportMeans" -> Json.arr(
            Json.obj(
              "sequenceNumber"                       -> 1234,
              "customsOfficeAtBorderReferenceNumber" -> "GB123456",
              "typeOfIdentification"                 -> "12",
              "identificationNumber"                 -> "string",
              "nationality"                          -> "GB",
              "conveyanceReferenceNumber"            -> "string"
            )
          ),
          "PlaceOfLoading" -> Json.obj(
            "UNLocode" -> "token",
            "country"  -> "GB",
            "location" -> "string"
          ),
          "HouseConsignment" -> Json.arr(
            Json.obj(
              "sequenceNumber" -> 123,
              "DepartureTransportMeans" -> Json.arr(
                Json.obj(
                  "sequenceNumber"       -> 1234,
                  "typeOfIdentification" -> "12",
                  "identificationNumber" -> "string",
                  "nationality"          -> "GB"
                )
              )
            )
          )
        ),
        "Representative" -> Json.obj(
          "identificationNumber" -> "string",
          "status"               -> "0",
          "ContactPerson" -> Json.obj(
            "name"         -> "string",
            "phoneNumber"  -> "token",
            "eMailAddress" -> "string"
          )
        ),
        "messageIdentification" -> "token"
      )
    )

    lazy val jsonInvalid =
      Json.obj(
        "n1:CC170C" ->
          Json.obj(
            "@PhaseID"      -> "NCTS5.0",
            "messageSender" -> "FdOcminxBxSLGm1rRUn0q96S1",
            "messageType"   -> "CC170C"
          )
      )
  }

}
