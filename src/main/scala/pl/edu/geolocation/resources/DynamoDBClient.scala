package pl.edu.geolocation.resources

import cats.effect.{Async, Resource}
import io.laserdisc.pure.dynamodb.tagless.{DynamoDbAsyncClientOp, Interpreter}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

object DynamoDBClient {

  def make[F[_]: Async](): Resource[F, DynamoDbAsyncClientOp[F]] = {
    Interpreter[F]
      .DynamoDbAsyncClientOpResource(DynamoDbAsyncClient.builder())
  }

}
