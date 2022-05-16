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
import org.scalatest.time.Span
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.transitmovementsvalidator.base.TestActorSystem
import uk.gov.hmrc.transitmovementsvalidator.models.errors.{SchemaValidationError, UnknownMessageTypeValidationError, ValidationError}

import java.nio.charset.StandardCharsets
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

class ValidationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures{

  implicit val timeout: Timeout           = Timeout(5.seconds)
  implicit val materializer: Materializer = Materializer(TestActorSystem.system)

  lazy val validXml: NodeSeq = <test></test>
  lazy val validCode: String = "IE015"

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 15.millis)

  "On Validate" - {
    "when valid XML is provided for the given message type, return a Right" in {
      val source = Source.single(ByteString(exampleIE015XML.mkString, StandardCharsets.UTF_8))
      val sut    = new ValidationServiceImpl
      val result = sut.validateXML(validCode, source)

      whenReady(result) {
        r =>
          r.isRight mustBe true
      }
    }

    "when no valid message type is provided, return UnknownMessageTypeValidationError" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val invalidCode = "dummy"
      val sut = new ValidationServiceImpl
      val result = sut.validateXML(invalidCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head mustBe ValidationError.fromUnrecognisedMessageType(invalidCode)
      }
    }

    "when valid message type provided but with unexpected xml, return errors" in {
      val source = Source.single(ByteString(validXml.mkString, StandardCharsets.UTF_8))
      val sut = new ValidationServiceImpl
      val result = sut.validateXML(validCode, source)

      whenReady(result) {
        r =>
          r.isLeft mustBe true
          r.left.get.head.isInstanceOf[SchemaValidationError]
      }
    }
  }

  val exampleIE015XML: NodeSeq =
    <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>Js8RQZM3NmgnNgNMM1T0NB0Cek</messageSender>
      <messageRecipient>7Pxs1c</messageRecipient>
      <preparationDateAndTime>2022-05-13T10:10:10</preparationDateAndTime>
      <messageIdentification>kqbgXzLp</messageIdentification>
      <messageType>CC015C</messageType>
      <TransitOperation>
        <LRN>N56geg</LRN>
        <declarationType>4hkXz</declarationType>
        <additionalDeclarationType>Q</additionalDeclarationType>
        <security>6</security>
        <reducedDatasetIndicator>1</reducedDatasetIndicator>
        <bindingItinerary>1</bindingItinerary>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>AP9PUS0C</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>AP9PUS0C</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <HolderOfTheTransitProcedure>
        <identificationNumber>eoriNumber</identificationNumber>
        <name>gtudjvWywUuNADDTFC4iYlHq9oQ</name>
        <Address>
          <streetAndNumber>Q9YOLKAgZUVCw3dGAmsW1EaGQGChFeASYBfOEMDJ6yfIPEFXkFt9rQ7mEUpZMO9ugU0j</streetAndNumber>
          <city>ICLAffZcVXJJYnjNRbW94</city>
          <country>GB</country>
        </Address>
        <ContactPerson>
          <name>gtudjvWywUuNADDTFC4iYlHq9oQ</name>
          <phoneNumber>EKJhhtXq</phoneNumber>
        </ContactPerson>
      </HolderOfTheTransitProcedure>
      <Guarantee>
        <sequenceNumber>51159</sequenceNumber>
        <guaranteeType>1</guaranteeType>
      </Guarantee>
      <Consignment>
        <grossMass>3006961631.286343</grossMass>
        <HouseConsignment>
          <sequenceNumber>51159</sequenceNumber>
          <grossMass>3006961631.286343</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
        <HouseConsignment>
          <sequenceNumber>51159</sequenceNumber>
          <grossMass>3006961631.286343</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
        <HouseConsignment>
          <sequenceNumber>51159</sequenceNumber>
          <grossMass>3006961631.286343</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
        <HouseConsignment>
          <sequenceNumber>51159</sequenceNumber>
          <grossMass>3006961631.286343</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
        <HouseConsignment>
          <sequenceNumber>51159</sequenceNumber>
          <grossMass>3006961631.286343</grossMass>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
          <ConsignmentItem>
            <goodsItemNumber>63423</goodsItemNumber>
            <declarationGoodsItemNumber>31271</declarationGoodsItemNumber>
            <Commodity>
              <descriptionOfGoods>KMtQqEvKoyfuizFrjg1uN9sp9rBMCnRejiSGIZfBSkbCB1p3zheS5fuh4AwpK0bbH3VyEgH67c4LKfGJpIdxAOCU5PGXUdiCNKy4RyERm832lEF8ABNKttotYgtHKqAEBe9XWArQTpzKnJ13mzV74uQFfwqB3i01z</descriptionOfGoods>
            </Commodity>
            <Packaging>
              <sequenceNumber>51159</sequenceNumber>
              <typeOfPackages>Z3</typeOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC015C>

}
