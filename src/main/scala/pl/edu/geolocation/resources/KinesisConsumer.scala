package pl.edu.geolocation.resources

import cats.effect.Async
import cats.effect.kernel.Resource
import fs2.aws.kinesis.Kinesis
import io.laserdisc.pure.cloudwatch.tagless.{Interpreter => CloudwatchInterpreter}
import io.laserdisc.pure.dynamodb.tagless.{Interpreter => DynamoDbInterpreter}
import io.laserdisc.pure.kinesis.tagless.{Interpreter => KinesisInterpreter}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

object KinesisConsumer {

  def make[F[_]: Async](): Resource[F, Kinesis[F]] =
    for {
      k <- KinesisInterpreter[F].KinesisAsyncClientResource(
        KinesisAsyncClient.builder()
      )
      d <- DynamoDbInterpreter[F].DynamoDbAsyncClientResource(
        DynamoDbAsyncClient.builder()
      )
      c <- CloudwatchInterpreter[F].CloudWatchAsyncClientResource(
        CloudWatchAsyncClient.builder()
      )
    } yield Kinesis.create[F](k, d, c)

}
