package pl.edu.geolocation.config

import cats.implicits._
import ciris.refined.refTypeConfigDecoder
import ciris.{ConfigValue, env}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

object S3CfgBuilder {

  final case class S3Cfg
  (
    bucketName: NonEmptyString,
    directory: NonEmptyString,
    recordsInFile: PosInt
  )

  def make[F[_]](): ConfigValue[F, S3Cfg] = {
    (
      env("BUCKET_NAME").as[NonEmptyString],
      env("DIRECTORY").as[NonEmptyString],
      env("RECORDS_IN_FILE").as[PosInt].default(PosInt(1000))
    ).parMapN(S3Cfg)
  }

}
