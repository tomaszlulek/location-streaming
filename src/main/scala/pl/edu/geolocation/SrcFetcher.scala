package pl.edu.geolocation

import cats.effect.{ExitCode, IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.LoggerFactory
import fs2.aws.kinesis.publisher.writeAndForgetObjectToKinesis
import pl.edu.geolocation.config.KinesisCfgBuilder
import pl.edu.geolocation.kinesis.LocationRecord
import pl.edu.geolocation.kinesis.LocationRecord.implicits._
import pl.edu.geolocation.sources.ztm.ZTMApi
import scala.language.postfixOps

object SrcFetcher extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory[IO].getLogger
    for {
      config <- KinesisCfgBuilder.make[IO]().load[IO]
      _ <- logger.info(s"Starting application publishing source stream to Kinesis stream ${config.kinesisStream}")
      _ <- ZTMApi.makeLocationSource[IO]().getStream
        .map(a => (a.id, a))
        .through(writeAndForgetObjectToKinesis[IO, LocationRecord](config.kinesisStream))
        .evalTap(a => logger.debug(s"Published location record: $a"))
        .compile
        .drain
    } yield ExitCode.Success
  }

}
