package pl.edu.geolocation

import cats.effect.IO.randomUUID
import cats.implicits._
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import eu.timepit.refined.types.string.NonEmptyString
import fs2.{Chunk, Stream}
import fs2.aws.kinesis.{
  CommittableRecord,
  Kinesis,
  KinesisCheckpointSettings,
  KinesisConsumerSettings
}
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.S3AsyncClientOp
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pl.edu.geolocation.config.KinesisCfgBuilder.KinesisCfg
import pl.edu.geolocation.config.S3CfgBuilder.S3Cfg
import pl.edu.geolocation.config.{KinesisCfgBuilder, S3CfgBuilder}
import pl.edu.geolocation.resources.{KinesisConsumer, S3Client}

import java.nio.charset.StandardCharsets

object Kinesis2S3 extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  private def getResources[F[_]: Async]
      : Resource[F, (Kinesis[F], S3AsyncClientOp[F])] = for {
    kinesis <- KinesisConsumer.make[F]()
    s3 <- S3Client.make[F]()
  } yield (kinesis, s3)

  private def getConfig[F[_]: Async]: F[(KinesisCfg, S3Cfg)] = for {
    kinesis <- KinesisCfgBuilder.make[F]().load[F]
    s3 <- S3CfgBuilder.make[F]().load[F]
  } yield (kinesis, s3)

  private def getFileFromChunk(
      records: Chunk[CommittableRecord]
  ): Option[String] = {
    records.toList
      .map(a => StandardCharsets.UTF_8.decode(a.record.data).toString)
      .reduceOption(_ + "\n" + _)
  }

  def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory[IO].getLogger
    getResources[IO].use { case (kinesis, s3Client) =>
      for {
        config <- getConfig[IO]
        kinesisCfg = config._1
        s3Cfg = config._2
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
          .chunkMin(s3Cfg.recordsInFile.value)
          .evalTap { records =>
            for {
              uuid <- randomUUID
              chunkFile = s3Cfg.directory.value + s"/$uuid.txt"
              jsons = getFileFromChunk(records)
              _ <-
                if (jsons.isDefined) {
                  Stream
                    .emits(jsons.get.getBytes)
                    .through(
                      s3.uploadFile(
                        BucketName(s3Cfg.bucketName),
                        FileKey(NonEmptyString.unsafeFrom(chunkFile))
                      )
                    )
                    .evalTap(t =>
                      logger.info(
                        s"Records batch (${s3Cfg.recordsInFile.value} items) stored in S3: $chunkFile (ETag $t)"
                      )
                    )
                    .compile
                    .drain
                } else IO.pure()
            } yield ()
          }
          .flatMap(Stream.chunk)
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
