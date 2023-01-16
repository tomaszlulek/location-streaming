package pl.edu.geolocation.resources

import cats.effect.{Async, Resource}
import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri.Scheme
import org.http4s.client.Client
import org.http4s.netty.client.{HttpProxy, NettyClientBuilder}
import pl.edu.geolocation.config.HttpClientCfgBuilder

object HttpClient {

  def make[F[_]: Async]: Resource[F, Client[F]] = {
    for {
      config <- HttpClientCfgBuilder.make().resource
      builder = NettyClientBuilder[F]
      afterProxy =
        if (config.proxyHost.isDefined && config.proxyPort.isDefined) {
          builder.withProxy(HttpProxy(
            Scheme.https,
            Host.fromString(config.proxyHost.get).get,
            Port.fromInt(config.proxyPort.get.value)
          ))
        }
        else builder
      client <- afterProxy.resource
    } yield client
  }

}
