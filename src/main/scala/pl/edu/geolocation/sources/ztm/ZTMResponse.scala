package pl.edu.geolocation.sources.ztm

import cats.effect.IO
import io.circe.Decoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import pl.edu.geolocation.sources.ztm.ZTMResponse.Vehicle
import io.circe.generic.auto._

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.util.Try

case class ZTMResponse
(
  result: List[Vehicle]
)

object ZTMResponse {

  case class Vehicle
  (
    Lines: String,
    VehicleNumber: String,
    Brigade: String,
    Lon: BigDecimal,
    Lat: BigDecimal,
    Time: Instant
  )

  object implicits {
    private val tsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
      Try(LocalDateTime.parse(str, tsFormatter).atZone(ZoneId.of("Europe/Warsaw")).toInstant)
    }
    implicit val responseDecoder: EntityDecoder[IO, ZTMResponse] = jsonOf[IO, ZTMResponse]
  }

}