package yogi.server

import org.scalatra.{PathPattern, SinatraPathPatternParser}
import spray.http.HttpResponse

import scala.collection.mutable.ListBuffer


class SubServer {

  private case class PathHandler(path: String, handler: (Request) => Unit, pathPattern: PathPattern)

  private val handlers: scala.collection.mutable.Map[HttpMethod, ListBuffer[PathHandler]] = scala.collection.mutable.Map.empty

  def get(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, GETRequest)
  }

  def post(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, POSTRequest)
  }

  def delete(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, DELETERequest)
  }

  def put(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, PUTRequest)
  }

  def patch(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, PATCHRequest)
  }

  def options(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, OPTIONSRequest)
  }

  def handle(request: Request) {
    val methodHandlers = handlers.get(request.method).getOrElse(ListBuffer.empty)
    val methodAndParams = methodHandlers.flatMap { x =>
      val multiParams = x.pathPattern(request.path)
      multiParams.map(a => (x, a))
    }.headOption
    methodAndParams match {
      case Some((handler, params)) => handler.handler(request)
      case _ =>  request.sender ! HttpResponse(404)
    }
  }

  private def addHandler(path: String, handler: (Request) => Unit, method: HttpMethod) {
    handlers.get(method) match {
      case Some(l) => l.append(PathHandler(path, handler, SinatraPathPatternParser(path)))
      case _ =>
        val l = ListBuffer[PathHandler]()
        l.append(PathHandler(path, handler, SinatraPathPatternParser(path)))
        handlers(method) = l
    }
  }

}