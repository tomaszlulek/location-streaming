package pl.edu.geolocation.sources.ztm

import org.http4s.{Method, Query, Request, Uri}

object ZTMRequest {

  sealed trait VehicleType {
    val id: String
  }

  case object BUS extends VehicleType {
    val id = "1"
  }

  case object TRAM extends VehicleType {
    val id = "2"
  }

  def make[F[_]](
      endpoint: String,
      resourceId: String,
      apiKey: String,
      vehicleType: VehicleType,
      line: Option[String] = None
  ): Request[F] = {
    val query = Query.fromMap(
      Map(
        "resource_id" -> Seq(resourceId),
        "apikey" -> Seq(apiKey),
        "type" -> Seq(vehicleType.id)
      ) ++ line.map(a => "line" -> Seq(a)).toSeq
    )
    val uri = Uri.fromString(endpoint).toOption.get.copy(query = query)
    Request[F](Method.POST, uri)
  }

}
