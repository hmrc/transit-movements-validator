package uk.gov.hmrc.transitmovementsvalidator.services

import com.eclipsesource.schema.SchemaType
import com.eclipsesource.schema.SchemaValidator
import com.eclipsesource.schema.drafts.Version7
import com.eclipsesource.schema.drafts.Version7._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.test.Helpers.contentAsString
import play.test.Helpers

import scala.io.Source

class JsonValidator extends AnyFreeSpec with Matchers with MockitoSugar {

  "On validate CC015c" - {
    "when valid json payload is provided should result in a successful validation" in new Setup {

      val payload = extractJson("c0015c-generated-from-json-schema.json")

      val actual: JsResult[JsValue] = validator.validate(schema, payload)
      val expected                  = JsSuccess("") // JsString("exceeds maximum length of 22")
      actual shouldBe a[JsSuccess[_]]
    }

    "when an invalid json payload is provided should result in an unsuccessful validation" in new Setup {

      val payload  = extractJson("c0015c-LRN-too-long.json")
      val actual   = validator.validate(schema, payload)
      val expected = "exceeds maximum length of 22"
      actual shouldBe a[JsError]
    }

  }

  trait Setup {

    val schema = Json
      .fromJson[SchemaType](extractJson("c0015c-schema.json"))
      .getOrElse(throw new IllegalStateException("Unable to extract Schema"))

    val validator = SchemaValidator(Some(Version7))

    def extractJson(filename: String): JsValue = {
      val in  = getClass.getResourceAsStream(s"/json/$filename")
      val raw = Source.fromInputStream(in).getLines.mkString
      Json.parse(raw)
    }
  }

}
