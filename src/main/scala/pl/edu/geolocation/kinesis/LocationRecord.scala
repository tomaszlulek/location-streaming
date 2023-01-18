package pl.edu.geolocation.kinesis

import io.circe.Encoder
import io.circe.syntax._
import io.circe.generic.semiauto._
import java.nio.ByteBuffer
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

case class LocationRecord(
    id: String,
    lat: BigDecimal,
    lon: BigDecimal,
    ts: Instant,
    params: Map[String, String],
    fetch_id: String,
    fetch_ts: Instant
)

object LocationRecord {

  private val formatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.of("UTC"))

  object implicits {
    implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString
      .contramap[Instant](instant => formatter.format(instant))
    implicit val fooEncoder: Encoder[LocationRecord] = deriveEncoder[LocationRecord]
    implicit val kinesisEncoder: LocationRecord => ByteBuffer = { a =>
      ByteBuffer.wrap(a.asJson.noSpaces.getBytes)
    }
  }
}