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

package uk.gov.hmrc.transitmovementsvalidator.services

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.errors.XmlSchemaValidationError
import uk.gov.hmrc.transitmovementsvalidator.models.errors.ValidationError

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

class XmlValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures {

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>
  lazy val validCode: String = "IE015"

  lazy val testDataPath = "./test/uk/gov/hmrc/transitmovementsvalidator/data"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate XML" - {
    "when valid XML is provided for the given message type, return a Right" in {
      val source = Source.single(ByteString(exampleIE015XML.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when no valid message type is provided, return UnknownMessageTypeValidationError" in {
      val source      = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val invalidCode = "dummy"
      val sut         = new XmlValidationServiceImpl
      val result      = sut.validate(invalidCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head mustBe ValidationError.fromUnrecognisedMessageType(invalidCode)
      }
    }

    "when valid message type provided but with unexpected xml, return errors" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val sut    = new XmlValidationServiceImpl
      val result = sut.validate(validCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head.isInstanceOf[XmlSchemaValidationError]
      }
    }
  }

  val exampleIE015XML: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageRecipient>3pekcCFaMGmCMz1CPGUlhyml9gJCV6</messageRecipient>
      <preparationDateAndTime>2022-07-02T03:11:04</preparationDateAndTime>
      <messageIdentification>wrxe</messageIdentification>
      <messageType>CC015C</messageType>
      <TransitOperation>
        <LRN>DHbrfgDQJRm</LRN>
        <declarationType>gJ</declarationType>
        <additionalDeclarationType>h</additionalDeclarationType>
        <security>7</security>
        <reducedDatasetIndicator>1</reducedDatasetIndicator>
        <bindingItinerary>1</bindingItinerary>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>BN3KMA8M</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>BN3KMA8M</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <HolderOfTheTransitProcedure>
        <identificationNumber>ezv3Z</identificationNumber>
      </HolderOfTheTransitProcedure>
      <Guarantee>
        <sequenceNumber>66710</sequenceNumber>
        <guaranteeType>P</guaranteeType>
        <otherGuaranteeReference>iNkM2E</otherGuaranteeReference>
      </Guarantee>
      <Consignment>
        <grossMass>4380979244.527545</grossMass>
        <HouseConsignment>
          <sequenceNumber>66710</sequenceNumber>
          <grossMass>4380979244.527545</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>34564</goodsItemNumber>
            <declarationGoodsItemNumber>25</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>fds9YFrlk6DX7pnwQNgJmksfZ4z9uGjDy6Kaucb13r3kEleTuLHD5zKtbAKUU005AaZeVdTgdAnJKzuGliZGRb1E83Y0Z8IuyeFfnXgT7NwX81eGFb3vRXAWUFswwwprqZBcffnBLwLObF45W7evl7C6J4Tihj1d1a2ZKcAU6ttLNy</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>66710</sequenceNumber>
              <typeOfPackages>Nu</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC015C>

}
