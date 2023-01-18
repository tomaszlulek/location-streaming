package pl.edu.geolocation

import cats.effect._
import fs2.aws.kinesis.{Kinesis, KinesisCheckpointSettings, KinesisConsumerSettings}
import io.laserdisc.pure.dynamodb.tagless.DynamoDbAsyncClientOp
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import pl.edu.geolocation.config.{DDBCfgBuilder, KinesisCfgBuilder}
import pl.edu.geolocation.ddb.DynamoDBWriter
import pl.edu.geolocation.resources.{DynamoDBClient, KinesisConsumer}

object Kinesis2DDB extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  private def getResources[F[_]: Async]
      : Resource[F, (Kinesis[F], DynamoDbAsyncClientOp[F])] = for {
    kinesis <- KinesisConsumer.make[F]()
    ddb <- DynamoDBClient.make[F]()
  } yield (kinesis, ddb)

  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: SelfAwareStructuredLogger[IO] =
      LoggerFactory[IO].getLogger
    getResources[IO].use { case (kinesis, ddb) =>
      for {
        kinesisCfg <- KinesisCfgBuilder.make[IO]().load
        ddbCfg <- DDBCfgBuilder.make[IO]().load
        _ <- logger.info(
          s"Starting application streaming data from Kinesis (${kinesisCfg.kinesisStream}) to DynamoDB"
        )
        _ <- kinesis
          .readFromKinesisStream(
            KinesisConsumerSettings(
              streamName = kinesisCfg.kinesisStream,
              appName = kinesisCfg.consumerName.get
            )
          )
          .through(DynamoDBWriter.makeKinesisWriter(ddb, ddbCfg))
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
