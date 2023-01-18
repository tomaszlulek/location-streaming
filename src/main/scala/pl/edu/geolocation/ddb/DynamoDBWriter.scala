package pl.edu.geolocation.ddb

import cats.Applicative
import cats.effect.{Async, Concurrent}
import cats.implicits._
import fs2.aws.kinesis.CommittableRecord
import fs2.{Pipe, Stream}
import io.circe
import io.circe.parser._
import io.laserdisc.pure.dynamodb.tagless.DynamoDbAsyncClientOp
import org.typelevel.log4cats.Logger
import pl.edu.geolocation.config.DDBCfgBuilder.DDBCfg
import pl.edu.geolocation.kinesis.LocationRecord
import pl.edu.geolocation.kinesis.LocationRecord.implicits._
import software.amazon.awssdk.services.dynamodb.model.{
  AttributeValue,
  BatchWriteItemRequest,
  PutRequest,
  WriteRequest
}

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._

object DynamoDBWriter {

  private def deserializeRecord(
      record: CommittableRecord
  ): Either[circe.Error, LocationRecord] =
    decode[LocationRecord](
      StandardCharsets.UTF_8.decode(record.record.data).toString
    )

  private def createWriteRequest(record: LocationRecord): WriteRequest = {
    val item = Map(
      "id" -> AttributeValue.builder.s(record.id).build,
      "ts" -> AttributeValue.builder.n(record.ts.getEpochSecond.toString).build,
      "lat" -> AttributeValue.builder.s(record.lat.toString).build,
      "lon" -> AttributeValue.builder.s(record.lat.toString).build
    )
    WriteRequest.builder
      .putRequest(PutRequest.builder.item(item.asJava).build)
      .build
  }

  def makeKinesisWriter[F[_]: Async: Logger](
      ddbClient: DynamoDbAsyncClientOp[F],
      ddbCfg: DDBCfg
  ): Pipe[F, CommittableRecord, CommittableRecord] = { in =>
    in
      .chunkLimit(ddbCfg.batchSize.value)
      .evalTap { chunk =>
        for {
          records <- chunk.toList
            .map(deserializeRecord)
            .pure[F]
          writeRequests = records.flatMap(_.toOption).map(createWriteRequest)
          _ <- Logger[F].info(
            s"Sending batch (${chunk.size} items) to DynamoDB"
          )
          _ <-
            if (writeRequests.nonEmpty)
              Concurrent[F].start(
                ddbClient.batchWriteItem(
                  BatchWriteItemRequest.builder
                    .requestItems(
                      Map(ddbCfg.tableName.value -> writeRequests.asJava).asJava
                    )
                    .build
                )
              )
            else Applicative[F].unit
        } yield ()
      }
      .flatMap(Stream.chunk)
  }

}
