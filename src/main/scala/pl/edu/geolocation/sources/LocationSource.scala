package pl.edu.geolocation.sources

import fs2.Stream
import pl.edu.geolocation.kinesis.LocationRecord

trait LocationSource[F[_]] {
  def getStream: Stream[F, LocationRecord]
}
