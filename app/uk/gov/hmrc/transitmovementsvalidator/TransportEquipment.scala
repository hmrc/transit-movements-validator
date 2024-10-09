package uk.gov.hmrc.transitmovementsvalidator

object TransportEquipment {

  def main(args: Array[String]): Unit =
    println("TransportEquipment....")  /// this takes 18 MB for 10 runs -- so for 9999 runs it will take around 18000 MB = 18 GB
  val xmlValue = new StringBuilder()

  for (i <- 0 to 10) {
    xmlValue.append("    <TransportEquipment>\n      <sequenceNumber>token</sequenceNumber>\n      <!--Optional:-->\n      <containerIdentificationNumber>string</containerIdentificationNumber>\n      <numberOfSeals>100</numberOfSeals>")
    xmlValue.append("\n<!--0 to 99 repetitions:-->\n")
    for (i <- 0 to 99) {
      xmlValue.append("      <Seal>\n        <sequenceNumber>token</sequenceNumber>\n        <identifier>string</identifier>\n      </Seal>")
    }
    xmlValue.append("\n<!--0 to 9999 repetitions:-->\n")
    for (i <- 0 to 9999) {
      xmlValue.append("      <GoodsReference>\n        <sequenceNumber>token</sequenceNumber>\n        <declarationGoodsItemNumber>100</declarationGoodsItemNumber>\n      </GoodsReference>")
    }
    xmlValue.append("</TransportEquipment>")
  }

  import java.nio.file.{Paths, Files}
  import java.nio.charset.StandardCharsets

  Files.write(Paths.get("TransportEquipment.xml"), xmlValue.toString().getBytes(StandardCharsets.UTF_8))

}
