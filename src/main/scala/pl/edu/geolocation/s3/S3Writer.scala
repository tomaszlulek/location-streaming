package pl.edu.geolocation.s3

import cats.Applicative
import cats.implicits._
import cats.effect.{Async, Clock}
import cats.effect.std.UUIDGen
import eu.timepit.refined.types.string.NonEmptyString
import fs2.{Chunk, Pipe, Stream}
import fs2.aws.kinesis.CommittableRecord
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import org.typelevel.log4cats.Logger
import pl.edu.geolocation.config.S3CfgBuilder.S3Cfg
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object S3Writer {

  private val formatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.of("UTC"))

  private def getChunkFileLocation[F[_]: Async](
      s3dir: NonEmptyString
  )(implicit clock: Clock[F]): F[NonEmptyString] = for {
    uuid <- UUIDGen[F].randomUUID
    currentTs <- clock.realTimeInstant
    chunkFile = s3dir.value + s"/load_dt=${formatter.format(currentTs)}/$uuid.txt"
  } yield NonEmptyString.unsafeFrom(chunkFile)

  private def getFileFromChunk(
      chunk: Chunk[CommittableRecord]
  ): Option[String] =
    chunk.toList
      .map(a => StandardCharsets.UTF_8.decode(a.record.data).toString)
      .reduceOption(_ + "\n" + _)

  def makeKinesisWriter[F[_]: Async: Logger](
      s3: S3[F],
      s3Cfg: S3Cfg
  ): Pipe[F, CommittableRecord, CommittableRecord] = { in =>
    in
      .chunkMin(s3Cfg.recordsInFile.value)
      .evalTap { chunk =>
        for {
          chunkFile <- getChunkFileLocation(s3Cfg.directory)
          file = getFileFromChunk(chunk)
          _ <-
            if (file.isDefined) {
              Stream
                .emits(file.get.getBytes)
                .through(
                  s3.uploadFile(
                    BucketName(s3Cfg.bucketName),
                    FileKey(chunkFile)
                  )
                )
                .evalTap(t =>
                  Logger[F].info(
                    s"Records batch (${chunk.size} items) stored in S3: $chunkFile (ETag $t)"
                  )
                )
                .compile
                .drain
            } else Applicative.pure[F]
        } yield ()
      }
      .flatMap(Stream.chunk)
  }

}
