package pl.edu.geolocation.sources.ztm

import cats.implicits._
import cats.effect.kernel.Async
import fs2.Stream
import org.http4s.client.Client
import pl.edu.geolocation.kinesis.LocationRecord
import pl.edu.geolocation.resources.HttpClient
import pl.edu.geolocation.sources.LocationSource
import pl.edu.geolocation.sources.ztm.ZTMRequest.{BUS, TRAM}

import scala.concurrent.duration._
import pl.edu.geolocation.sources.ztm.ZTMResponse.Vehicle
import pl.edu.geolocation.sources.ztm.ZTMResponse.implicits._

object ZTMApi {

  private def makeAPIStream[F[_] : Async]
  (
      httpClient: Client[F]
  ): Stream[F, Vehicle] = {
    Stream
      .eval(ZTMCfgBuilder.make[F]().load)
      .flatMap { config =>
        Stream
          .awakeDelay[F](config.apiFrequencySeconds.seconds)
          .evalMap { _ =>
            for {
              buses <- httpClient.expectOption[ZTMResponse](
                ZTMRequest.make[F](
                  config.endpoint,
                  config.resourceId,
                  config.apiKey.value,
                  BUS
                )
              )
              trams <- httpClient.expectOption[ZTMResponse](
                ZTMRequest.make[F](
                  config.endpoint,
                  config.resourceId,
                  config.apiKey.value,
                  TRAM
                )
              )
              vehicles = buses.toList.flatMap(_.result) ++ trams.toList.flatMap(_.result)
            } yield vehicles
          }
      }
      .flatMap(Stream.emits(_))
  }

  def makeLocationSource[F[_] : Async](): LocationSource[F] = new LocationSource[F] {
    def getStream: Stream[F, LocationRecord] =
      Stream
        .resource(HttpClient.make[F])
        .flatMap(makeAPIStream[F](_))
        .map(vehicle => LocationRecord(
          id = vehicle.Lines,
          lat = vehicle.Lat,
          lon = vehicle.Lon,
          ts = vehicle.Time,
          params = Map(
            "VehicleNumber" -> vehicle.VehicleNumber,
            "Brigade" -> vehicle.Brigade
          )
        )
        )
  }

}
