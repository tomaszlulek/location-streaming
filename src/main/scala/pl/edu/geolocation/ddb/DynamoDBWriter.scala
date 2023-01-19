package pl.edu.geolocation.ddb

import cats.Applicative
import cats.effect.Async
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
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, BatchWriteItemRequest, PutRequest, WriteRequest}

import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._

object DynamoDBWriter {

  private val formatter = DateTimeFormatter
    .ofPattern("yyyyMMddHHmmss")

  private def deserializeRecord(
      record: CommittableRecord
  ): Either[circe.Error, LocationRecord] =
    decode[LocationRecord](
      StandardCharsets.UTF_8.decode(record.record.data).toString
    )

  private def createWriteRequest(record: LocationRecord): WriteRequest = {
    val item = Map(
      "business_id" -> AttributeValue.builder.s(record.business_id).build,
      "ts_with_unique_id" -> AttributeValue.builder
        .s(
          formatter.format(
            record.ts.atZone(ZoneId.of("Europe/Warsaw"))
          ) + ":" + record.unique_id
        )
        .build,
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
      .parEvalMap(ddbCfg.parallelism.value) { chunk =>
        for {
          chunk <- chunk.pure[F]
          records = chunk.toList
            .map(deserializeRecord)
          writeRequests = records.flatMap(_.toOption).map(createWriteRequest)
          _ <- Logger[F].debug(
            s"Sending batch (${chunk.size} items) to DynamoDB"
          )
          _ <-
            if (writeRequests.nonEmpty)
              ddbClient.batchWriteItem(
                BatchWriteItemRequest.builder
                  .requestItems(
                    Map(ddbCfg.tableName.value -> writeRequests.asJava).asJava
                  )
                  .build
              )
            else Applicative[F].unit
          _ <- Logger[F].debug(
            s"Sent batch (${chunk.size} items) to DynamoDB"
          )
        } yield chunk
      }
      .flatMap(Stream.chunk)
  }

}
