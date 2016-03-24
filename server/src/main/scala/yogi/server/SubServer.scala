package yogi.server

import org.scalatra.{PathPattern, SinatraPathPatternParser}
import spray.http.HttpResponse

import scala.collection.mutable.ListBuffer

// A component of a server, contained to a path
class SubServer {

  private case class PathHandler(path: String, handler: (Request) => Unit, pathPattern: PathPattern)

  private val handlers: scala.collection.mutable.Map[HttpMethod, ListBuffer[PathHandler]] = scala.collection.mutable.Map.empty

  /**
   * Handles a get request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def get(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.GET)
  }

  /**
   * Handles a post request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def post(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.POST)
  }

  /**
   * Handles a delete request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def delete(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.DELETE)
  }

  /**
   * Handles a put request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def put(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.PUT)
  }

  /**
   * Handles a get request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def patch(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.PATCH)
  }

  /**
   * Handles a options request
   *
   * @param path a path pattern
   * @param handler a callback
   */
  def options(path: String)(handler: (Request) => Unit) {
    addHandler(path, handler, yogi.server.OPTIONS)
  }


  /**
   * Route an incoming request
   * @param request a request
   */
  def route(request: Request) {
    val methodHandlers = handlers.get(request.method).getOrElse(ListBuffer.empty)
    val methodAndParams = methodHandlers.flatMap { x =>
      val multiParams = x.pathPattern(request.path)
      multiParams.map(a => (x, a))
    }.headOption
    methodAndParams match {
      case Some((handler, params)) => handler.handler(request.copy(params = params))
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