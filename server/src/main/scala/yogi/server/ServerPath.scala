package yogi.server

import akka.http.scaladsl.model.{HttpMethod, HttpResponse, HttpRequest}
import akka.http.scaladsl.model.HttpMethods._

import com.netaporter.uri.Uri

import org.scalatra.{MultiParams, PathPattern, SinatraPathPatternParser}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


sealed trait RequestHandler

case class Async(handler: (HttpRequest, MultiParams) => Future[HttpResponse]) extends RequestHandler
case class Sync(handler: (HttpRequest, MultiParams) => HttpResponse) extends RequestHandler


class HandlerSelector(path: String) {

  private[yogi] var requestHandler:RequestHandler = Sync((a, b) => HttpResponse(404))
  val pathPattern: PathPattern = SinatraPathPatternParser(path)

  def async(handler: (HttpRequest, MultiParams) => Future[HttpResponse]) {
    requestHandler = Async(handler)
  }

  def sync(handler: (HttpRequest, MultiParams) => HttpResponse) {
    requestHandler = Sync(handler)
  }

  def handle(request: HttpRequest, params: MultiParams) : Future[HttpResponse] = {

    requestHandler match {
      case Async(h) => h(request, params)
      case Sync(h) =>  Future.successful(h(request, params))
    }
  }

}


// A component of a server, contained to a path
class ServerPath {

  private val handlers: scala.collection.mutable.Map[HttpMethod, ListBuffer[HandlerSelector]] = scala.collection.mutable.Map.empty
  
  /**
   * Handles a get request
   *
   * @param path a path pattern
   */
  def get(path: String): HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, GET)
    selector
  }

  /**
   * Handles a post request
   *
   * @param path a path pattern
   */
  def post(path: String): HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, POST)
    selector
  }

  /**
   * Handles a delete request
   *
   * @param path a path pattern
   */
  def delete(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, DELETE)
    selector
  }

  /**
   * Handles a put request
   *
   * @param path a path pattern
   */
  def put(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, PUT)
    selector
  }

  /**
   * Handles a get request
   *
   * @param path a path pattern
   */
  def patch(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, PATCH)
    selector
  }

  /**
   * Handles a options request
   *
   * @param path a path pattern
   */
  def options(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, OPTIONS)
    selector
  }


  /**
   * Route an incoming request
   * @param request a request
   */
  def route(request: HttpRequest, adjustedPath: String) : Future[HttpResponse] = {

    val methodHandlers = handlers.get(request.method).getOrElse(ListBuffer.empty)
    val methodAndParams = methodHandlers.flatMap { x =>
      val multiParams = x.pathPattern(adjustedPath)
      multiParams.map(a => (x, a))
    }.headOption
    methodAndParams match {
      case Some((handler, params)) =>
        val x = Uri.parse(request.getUri().toString).query.paramMap
        var finalParams = params
        x.foreach { case (k, l) =>
          finalParams = finalParams + ((k, l))
        }
        handler.handle(request, finalParams)
      case _ =>  Future.successful(HttpResponse(404))
    }
  }

  protected def addHandler(path: String, handler: HandlerSelector, method: HttpMethod) {
    handlers.get(method) match {
      case Some(l) => l.append(handler)
      case _ =>
        val l = ListBuffer[HandlerSelector]()
        l.append(handler)
        handlers(method) = l
    }
  }

}