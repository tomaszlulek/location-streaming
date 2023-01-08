package pl.edu.geolocation.resources

import cats.effect.{Async, Resource}
import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri.Scheme
import org.http4s.client.Client
import org.http4s.netty.client.{HttpProxy, NettyClientBuilder}

object HttpClient {

  def make[F[_]: Async](proxyHost: Option[String], proxyPort: Option[Int]): Resource[F, Client[F]] = {
    val builder = NettyClientBuilder[F]
    val afterProxy =
      if(proxyHost.isDefined && proxyPort.isDefined) {
        builder.withProxy(HttpProxy(
          Scheme.https,
          Host.fromString(proxyHost.get).get,
          Port.fromInt(proxyPort.get)
        ))
      }
      else builder
    afterProxy.resource
  }

}
