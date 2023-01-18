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
      env("ENDPOINT").as[String],
      env("RESOURCE_ID").as[String],
      env("API_KEY").as[String].secret,
      env("API_FREQUENCY_SECONDS").as[Long].default(60)
    ).parMapN(SrcFetcherCfg)
  }
}
