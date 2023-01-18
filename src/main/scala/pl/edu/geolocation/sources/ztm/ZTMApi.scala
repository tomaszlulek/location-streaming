package pl.edu.geolocation.sources.ztm

import cats.effect.Clock
import cats.effect.kernel.Async
import cats.effect.std.UUIDGen
import cats.implicits._
import fs2.{Pipe, Stream}
import org.http4s.client.Client
import pl.edu.geolocation.config.ZTMCfgBuilder
import pl.edu.geolocation.kinesis.LocationRecord
import pl.edu.geolocation.resources.HttpClient
import pl.edu.geolocation.sources.LocationSource
import pl.edu.geolocation.sources.ztm.ZTMRequest.{BUS, TRAM}
import pl.edu.geolocation.sources.ztm.ZTMResponse.Vehicle
import pl.edu.geolocation.sources.ztm.ZTMResponse.implicits._

import scala.concurrent.duration._

object ZTMApi {

  private def makeAPIStream[F[_]: Async](
      httpClient: Client[F]
  ): Stream[F, List[Vehicle]] = {
    Stream
      .eval(ZTMCfgBuilder.make[F]().load)
      .flatMap { config =>
        Stream
          .awakeDelay[F](config.apiFrequencySeconds.seconds)
          .evalMap { _ =>
            for {
              buses <- httpClient
                .expect[ZTMResponse](
                  ZTMRequest.make[F](
                    config.endpoint,
                    config.resourceId,
                    config.apiKey.value,
                    BUS
                  )
                )
                .attempt
              trams <- httpClient
                .expect[ZTMResponse](
                  ZTMRequest.make[F](
                    config.endpoint,
                    config.resourceId,
                    config.apiKey.value,
                    TRAM
                  )
                )
                .attempt
              vehicles = buses.toOption.toList
                .flatMap(_.result) ++ trams.toOption.toList.flatMap(_.result)
            } yield vehicles
          }
      }
  }

  private def createLocationRecords[F[_]: Async](implicit
      clock: Clock[F]
  ): Pipe[F, List[Vehicle], List[LocationRecord]] =
    _.evalMap { vehicles =>
      for {
        uuid <- UUIDGen[F].randomUUID
        currentTs <- clock.realTimeInstant
      } yield vehicles.map(vehicle =>
        LocationRecord(
          id = vehicle.Lines,
          lat = vehicle.Lat,
          lon = vehicle.Lon,
          ts = vehicle.Time,
          params = Map(
            "VehicleNumber" -> vehicle.VehicleNumber,
            "Brigade" -> vehicle.Brigade
          ),
          fetch_id = uuid.toString,
          fetch_ts = currentTs
        )
      )
    }

  def makeLocationSource[F[_]: Async: Clock](): LocationSource[F] =
    new LocationSource[F] {
      def getStream: Stream[F, LocationRecord] =
        Stream
          .resource(HttpClient.make[F])
          .flatMap(makeAPIStream[F](_))
          .through(createLocationRecords)
          .flatMap(Stream.emits(_))
    }

}
