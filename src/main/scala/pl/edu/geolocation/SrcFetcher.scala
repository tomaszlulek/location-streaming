package pl.edu.geolocation

import cats.effect.{ExitCode, IO, IOApp}
import com.banno.kafka.{BootstrapServers, stringSerializer}
import com.banno.kafka.producer.ProducerApi
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pl.edu.geolocation.sources.ztm.{ZTMApi, ZTMResponse}
import pl.edu.geolocation.sources.ztm.ZTMResponse.implicits._
import org.typelevel.log4cats.LoggerFactory
import pl.edu.geolocation.config.SrcFetcherCfgBuilder
import pl.edu.geolocation.resources.HttpClient
import fs2.Stream
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.duration._
import scala.language.postfixOps

object SrcFetcher extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory[IO].getLogger
    SrcFetcherCfgBuilder.make[IO]().load.flatMap { config =>
      val resources = for {
        a <- ProducerApi.resource[IO, String, String](
          BootstrapServers("kafkaBootstrapServers")
        )
        b <- HttpClient
          .make[IO](config.proxyHost, config.proxyPort.map(_.value))
      } yield (a, b)
      Stream
        .resource(resources)
        .flatMap { case (kafkaProducer, httpClient) =>
          Stream
            .awakeDelay[IO](1 second)
            .evalMap { _ =>
              for {
                buses <- httpClient.expect[ZTMResponse](
                  ZTMApi.make[IO](
                    config.endpoint,
                    config.resourceId,
                    config.apiKey.value,
                    ZTMApi.BUS
                  )
                )
                trams <- httpClient.expect[ZTMResponse](
                  ZTMApi.make[IO](
                    config.endpoint,
                    config.resourceId,
                    config.apiKey.value,
                    ZTMApi.TRAM
                  )
                )
                vehicles = buses.result ++ trams.result
                _ <- logger.info(vehicles.toString)
                _ <- kafkaProducer
                  .sendAndForget(new ProducerRecord("topic.name", "i", "i"))
              } yield ()
            }
        }
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
