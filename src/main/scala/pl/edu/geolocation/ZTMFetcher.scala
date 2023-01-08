package pl.edu.geolocation

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pl.edu.geolocation.sources.ztm.{ZTMApi, ZTMResponse}
import pl.edu.geolocation.sources.ztm.ZTMResponse.implicits._
import org.typelevel.log4cats.LoggerFactory
import pl.edu.geolocation.resources.HttpClient

object ZTMFetcher extends IOApp{

  private val proxyHost = ""
  private val proxyPort = 3128
  private val endpoint = "https://api.um.warszawa.pl/api/action/busestrams_get/"
  private val resourceId = "f2e5503e927d-4ad3-9500-4ab9e55deb59"
  private val apiKey = ""

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory[IO].getLogger
    HttpClient
      .make[IO](proxyHost.some, proxyPort.some)
      .use { client =>
      for {
        resp <- client.expect[ZTMResponse](ZTMApi.make[IO](endpoint, resourceId, apiKey, ZTMApi.TRAM))
        _ <- logger.info(resp.toString)
      } yield ExitCode.Success
    }
  }

}
