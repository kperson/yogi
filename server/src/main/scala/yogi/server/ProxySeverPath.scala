package yogi.server


import akka.http.scaladsl.model._

import com.netaporter.uri.Uri

import java.io.File

import scala.concurrent.ExecutionContext

import yogi.client.Request


class ProxySeverPath(origin: String)(implicit ec: ExecutionContext) extends ServerPath {

  val root = if(origin.endsWith("/")) origin else origin + "/"


  private def suffix(path: String): String = path.reverse.takeWhile(_ != '.').reverse


  get("*").async { (req, params) =>
    val path = params("splat").headOption.getOrElse("/").split("/").mkString(File.separator)
    val adjustedPath = if(path.startsWith("/")) path.substring(1, path.length) else path
    val mediaType = if(path.contains("."))  MediaTypes.forExtension(suffix(adjustedPath)) else  MediaTypes.forExtension("html")

    val queryParams = com.netaporter.uri.Uri.parse(req.getUri().toString).queryStringRaw

    val finalEndpoint = queryParams match {
      case "" => root + adjustedPath
      case x => root + adjustedPath + "?" + x
    }
    println(finalEndpoint)
    Request.get(finalEndpoint).map {
      case x =>
        val contentType = ContentType(mediaType, () => HttpCharsets.`UTF-8`)
        HttpResponse(entity = HttpEntity(contentType, x.getResponseBodyAsBytes), status = x.getStatusCode)
    }.recover {
      case _ => HttpResponse(status = 500)
    }
  }

}
