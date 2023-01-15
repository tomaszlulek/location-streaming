package pl.edu.geolocation.resources

import cats.implicits._
import ciris.refined._
import ciris.{ConfigValue, env}
import eu.timepit.refined.types.net.PortNumber
object HttpClientCfgBuilder {

  final case class HttpClientCfg
  (
    proxyHost: Option[String],
    proxyPort: Option[PortNumber]
  )

  def make[F[_]](): ConfigValue[F, HttpClientCfg] = {
    (
      env("PROXY_HOST").as[String].option,
      env("PROXY_PORT").as[PortNumber].option
    ).parMapN(HttpClientCfg)
  }
}
