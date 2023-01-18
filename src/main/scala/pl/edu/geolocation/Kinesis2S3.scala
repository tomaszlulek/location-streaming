package pl.edu.geolocation

import cats.effect._
import fs2.aws.kinesis.{Kinesis, KinesisCheckpointSettings, KinesisConsumerSettings}
import fs2.aws.s3.S3
import io.laserdisc.pure.s3.tagless.S3AsyncClientOp
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import pl.edu.geolocation.config.{KinesisCfgBuilder, S3CfgBuilder}
import pl.edu.geolocation.resources.{KinesisConsumer, S3Client}
import pl.edu.geolocation.s3.S3Writer

object Kinesis2S3 extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  private def getResources[F[_]: Async]
      : Resource[F, (Kinesis[F], S3AsyncClientOp[F])] = for {
    kinesis <- KinesisConsumer.make[F]()
    s3 <- S3Client.make[F]()
  } yield (kinesis, s3)

  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: SelfAwareStructuredLogger[IO] =
      LoggerFactory[IO].getLogger
    getResources[IO].use { case (kinesis, s3Client) =>
      for {
        kinesisCfg <- KinesisCfgBuilder.make[IO]().load
        s3Cfg <- S3CfgBuilder.make[IO]().load
        s3 <- S3.create(s3Client)
        _ <- logger.info(
          s"Starting application streaming data from Kinesis (${kinesisCfg.kinesisStream}) to S3"
        )
        _ <- kinesis
          .readFromKinesisStream(
            KinesisConsumerSettings(
              streamName = kinesisCfg.kinesisStream,
              appName = kinesisCfg.consumerName.get
            )
          )
          .through(S3Writer.makeKinesisWriter(s3, s3Cfg))
          .through(
            kinesis.checkpointRecords(KinesisCheckpointSettings.defaultInstance)
          )
          .evalTap(cr => logger.debug(s"Finished processing record $cr"))
          .compile
          .drain
      } yield ExitCode.Success
    }
  }

}
