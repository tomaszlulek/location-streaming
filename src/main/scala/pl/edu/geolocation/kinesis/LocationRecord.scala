package pl.edu.geolocation.kinesis

import io.circe.syntax._
import io.circe.generic.auto._
import java.nio.ByteBuffer
import java.time.Instant

case class LocationRecord(
    id: String,
    lat: BigDecimal,
    lon: BigDecimal,
    ts: Instant,
    params: Map[String, String]
)

object LocationRecord {
  object implicits {
    implicit val kinesisEncoder: LocationRecord => ByteBuffer = { a =>
      ByteBuffer.wrap(a.asJson.noSpaces.getBytes)
    }
  }
}