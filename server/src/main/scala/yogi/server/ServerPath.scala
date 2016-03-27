package yogi.server

import org.scalatra.{PathPattern, SinatraPathPatternParser}
import spray.http.HttpResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


sealed trait RequestHandler

case class Async(handler: (Request) => Future[HttpResponse]) extends RequestHandler
case class Sync(handler: (Request) => HttpResponse) extends RequestHandler
case class Raw(handler: (Request) => Any) extends RequestHandler


class HandlerSelector(path: String) {

  private var requestHandler:RequestHandler = Raw((a) => Unit)
  val pathPattern: PathPattern = SinatraPathPatternParser(path)

  def async(handler: (Request) => Future[HttpResponse]) {
    requestHandler = Async(handler)
  }

  def sync(handler: (Request) => HttpResponse) {
    requestHandler = Sync(handler)
  }

  def raw(handler: (Request) => Any) {
    requestHandler = Raw(handler)
  }

  def handle(request: Request) {
    import request.context.dispatcher
    requestHandler match {
      case Async(h) =>
        val gather = h(request)
        gather.onSuccess {
          case res => request.sender ! res
        }
        gather.onFailure {
          case ex =>
            System.err.print("context fetch failed")
            println(ex.getMessage)
            ex.printStackTrace()
            request.sender ! HttpResponse(500)
        }
      case Sync(h) => request.sender ! h(request)
      case Raw(h) => h(request)
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
    addHandler(path, selector, yogi.server.GET)
    selector
  }

  /**
   * Handles a post request
   *
   * @param path a path pattern
   */
  def post(path: String): HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, yogi.server.POST)
    selector
  }

  /**
   * Handles a delete request
   *
   * @param path a path pattern
   */
  def delete(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, yogi.server.DELETE)
    selector
  }

  /**
   * Handles a put request
   *
   * @param path a path pattern
   */
  def put(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, yogi.server.PUT)
    selector
  }

  /**
   * Handles a get request
   *
   * @param path a path pattern
   */
  def patch(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, yogi.server.PATCH)
    selector
  }

  /**
   * Handles a options request
   *
   * @param path a path pattern
   */
  def options(path: String) : HandlerSelector = {
    val selector = new HandlerSelector(path)
    addHandler(path, selector, yogi.server.OPTIONS)
    selector
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
      case Some((handler, params)) =>
        val req = request.copy(params = params)
        handler.handle(req)
      case _ =>  request.sender ! HttpResponse(404)
    }
  }

  private def addHandler(path: String, handler: HandlerSelector, method: HttpMethod) {
    handlers.get(method) match {
      case Some(l) => l.append(handler)
      case _ =>
        val l = ListBuffer[HandlerSelector]()
        l.append(handler)
        handlers(method) = l
    }
  }

}