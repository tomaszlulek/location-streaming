package pl.edu.geolocation.sources.ztm

import cats.effect.kernel.Concurrent
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import pl.edu.geolocation.sources.ztm.ZTMResponse.Vehicle

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}
import scala.util.Try

case class ZTMResponse(
    result: List[Vehicle]
)

object ZTMResponse {

  case class Vehicle(
      Lines: String,
      VehicleNumber: String,
      Brigade: String,
      Lat: BigDecimal,
      Lon: BigDecimal,
      Time: Instant
  )

  object implicits {
    private val tsFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    implicit val decodeInstant: Decoder[Instant] =
      Decoder.decodeString.emapTry { str =>
        Try(
          LocalDateTime
            .parse(str, tsFormatter)
            .atZone(ZoneId.of("Europe/Warsaw"))
            .toInstant
        )
      }
    implicit def responseDecoder[F[_]: Concurrent]
        : EntityDecoder[F, ZTMResponse] = jsonOf[F, ZTMResponse]
  }

}
