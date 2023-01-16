package pl.edu.geolocation

import cats.effect.{ExitCode, IO, IOApp}
import fs2.aws.kinesis.{KinesisCheckpointSettings, KinesisConsumerSettings}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pl.edu.geolocation.config.KinesisCfgBuilder
import pl.edu.geolocation.resources.KinesisConsumer
import java.nio.charset.StandardCharsets

object Kinesis2S3 extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]
//
//  Stream.emits("test data".getBytes("UTF-8"))
//    .through(s3.uploadFile(BucketName("foo"), FileKey("bar"), partSize = 5))
//    .evalMap(t => IO(println(s"eTag: $t")))

  def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory[IO].getLogger
    KinesisConsumer.make[IO]().use { kinesis =>
      for {
        config <- KinesisCfgBuilder.make[IO]().load[IO]
        _ <- logger.info(
          s"Starting application streaming data from Kinesis (${config.kinesisStream}) to S3"
        )
        _ <- kinesis
          .readFromKinesisStream(
            KinesisConsumerSettings(
              streamName = config.kinesisStream,
              appName = config.consumerName.get
            )
          )
          .through(
            kinesis.checkpointRecords(KinesisCheckpointSettings.defaultInstance)
          )
          .evalTap(cr =>
            logger.info(
              s"Finished processing record ${StandardCharsets.UTF_8.decode(cr.data()).toString}"
            )
          )
          .compile
          .drain
      } yield ExitCode.Success
    }
  }

}
