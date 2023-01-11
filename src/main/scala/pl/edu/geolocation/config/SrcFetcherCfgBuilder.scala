package pl.edu.geolocation.config

import cats.implicits._
import ciris.{ConfigValue, Secret, env}
import eu.timepit.refined.types.net.PortNumber
import ciris.refined._
object SrcFetcherCfgBuilder {

  final case class SrcFetcherCfg
  (
    proxyHost: Option[String],
    proxyPort: Option[PortNumber],
    endpoint: String,
    resourceId: String,
    apiKey: Secret[String]
  )

  def make[F[_]](): ConfigValue[F, SrcFetcherCfg] = {
    (
      env("PROXY_HOST").as[String].option,
      env("PROXY_PORT").as[PortNumber].option,
      env("ENDPOINT").as[String],
      env("RESOURCE_ID").as[String],
      env("API_KEY").as[String].secret
    ).parMapN(SrcFetcherCfg)
  }
}
