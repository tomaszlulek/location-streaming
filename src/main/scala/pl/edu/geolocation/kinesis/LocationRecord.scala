package pl.edu.geolocation.kinesis

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

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
    implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString
      .map(a => Instant.from(LocalDateTime.parse(a, formatter).atZone(ZoneId.of("UTC"))))
    implicit val locationRecordEncoder: Encoder[LocationRecord] =
      deriveEncoder[LocationRecord]
    implicit val locationRecordDecoder: Decoder[LocationRecord] =
      deriveDecoder[LocationRecord]
    implicit val kinesisEncoder: LocationRecord => ByteBuffer = { a =>
      ByteBuffer.wrap(a.asJson.noSpaces.getBytes)
    }
  }
}
