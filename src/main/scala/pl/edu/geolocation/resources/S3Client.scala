package pl.edu.geolocation.resources

import cats.effect.{Async, Resource}
import io.laserdisc.pure.s3.tagless.{S3AsyncClientOp, Interpreter => S3Interpreter}
import software.amazon.awssdk.services.s3.S3AsyncClient

object S3Client {

  def make[F[_]: Async](): Resource[F, S3AsyncClientOp[F]] = {
    S3Interpreter[F]
      .S3AsyncClientOpResource(S3AsyncClient.builder())
  }

}
