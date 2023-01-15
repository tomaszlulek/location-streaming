package pl.edu.geolocation.config

import ciris.{ConfigValue, env}

object SrcFetcherCfgBuilder {

  final case class SrcFetcherCfg
  (
    kinesisStream: String
  )

  def make[F[_]](): ConfigValue[F, SrcFetcherCfg] =
    env("KINESIS_STREAM").as[String].map(SrcFetcherCfg)

}
