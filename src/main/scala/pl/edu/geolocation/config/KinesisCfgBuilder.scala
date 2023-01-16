package pl.edu.geolocation.config

import cats.implicits._
import ciris.{ConfigValue, env}

object KinesisCfgBuilder {

  final case class KinesisCfg
  (
    kinesisStream: String,
    consumerName: Option[String]
  )

  def make[F[_]](): ConfigValue[F, KinesisCfg] = {
    (
      env("KINESIS_STREAM").as[String],
      env("CONSUMER_NAME").as[String].option
    ).parMapN(KinesisCfg)
  }

}
