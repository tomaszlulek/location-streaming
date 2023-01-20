package pl.edu.geolocation.config

import cats.implicits._
import ciris.{ConfigValue, Secret, env}

object ZTMCfgBuilder {

  final case class SrcFetcherCfg(
      endpoint: String,
      resourceId: String,
      apiKey: Secret[String],
      apiFrequencySeconds: Long
  )

  def make[F[_]](): ConfigValue[F, SrcFetcherCfg] = {
    (
      env("ZTM_ENDPOINT").as[String],
      env("ZTM_RESOURCE_ID").as[String],
      env("ZTM_API_KEY").as[String].secret,
      env("ZTM_API_FREQUENCY_SECONDS").as[Long].default(60)
    ).parMapN(SrcFetcherCfg)
  }
}
