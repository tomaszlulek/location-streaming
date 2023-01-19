package pl.edu.geolocation.config

import cats.implicits._
import ciris.refined.refTypeConfigDecoder
import ciris.{ConfigValue, env}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

object DDBCfgBuilder {

  final case class DDBCfg(
      tableName: NonEmptyString,
      parallelism: PosInt,
      batchSize: PosInt
  )

  def make[F[_]](): ConfigValue[F, DDBCfg] = {
    (
      env("DDB_TABLE_NAME").as[NonEmptyString],
      env("DDB_PARALLELISM").as[PosInt].default(PosInt(1)),
      env("DDB_BATCH_SIZE").as[PosInt].default(PosInt(25))
    ).parMapN(DDBCfg)
  }

}
