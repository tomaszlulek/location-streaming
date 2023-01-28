package pl.edu.geolocation.sources.random

import java.time.Instant
import java.util.UUID.randomUUID
import cats.effect.kernel.Async
import fs2.Stream
import pl.edu.geolocation.kinesis.LocationRecord
import pl.edu.geolocation.sources.LocationSource

import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps

object RandomSource {

  private def getRandomLocation(businessId: Int, fetchId: String, fetchTs: Instant): LocationRecord = {
    LocationRecord(
      unique_id = randomUUID.toString,
      business_id = businessId.toString,
      lat = BigDecimal(Random.between(-90.0, 90.0)),
      lon = BigDecimal(Random.between(-180.0, 180.0)),
      ts = Instant.now,
      params = Map.empty,
      fetch_id = fetchId,
      fetch_ts = fetchTs
    )
  }

  def make[F[_] : Async]: LocationSource[F] = new LocationSource[F] {
    def getStream: Stream[F, LocationRecord] = Stream
      .awakeDelay[F](1 seconds)
      .flatMap(_ => Stream.emits {
        1 to 100 map (id => getRandomLocation(id, randomUUID.toString, Instant.now))
      })
  }

}
