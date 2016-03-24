package yogi.server

import akka.actor._
import akka.io.IO
import akka.pattern.ask

import spray.can.Http
import spray.http.{HttpResponse, ChunkedRequestStart, Uri, HttpRequest}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._


class Server(interface: String, port: Int)(implicit val system: ActorSystem) {

  case class RoutePath(path: String, subServer: SubServer)

  private[yogi] val routePaths: ListBuffer[RoutePath] = ListBuffer.empty
  private var handler: Option[ActorRef] = None

  /**
   * Adds a sub server contain to a path
   *
   * @param path the containing path
   * @param subServer the sub server
   */
  def addRoute(path: String, subServer: SubServer) {
    routePaths.prepend(RoutePath(path, subServer))
  }

  /**
   * Adds a sub server to the root
   *
   * @param subServer the sub server
   */
  def addRoute(subServer: SubServer) {
    routePaths.append(RoutePath("", subServer))
  }

  /**
   * Starts the server
   */
  def start() {
    val h = system.actorOf(Props(new ServerHandler(this)))
    (IO(Http) ? Http.Bind(h, interface = interface, port = port))(akka.util.Timeout(10.seconds))
    handler = Some(h)
  }

  /**
   * Stops the server
   */
  def stop() {
    handler.foreach(system.stop(_))
  }

}

class ServerHandler[yogi](server: Server) extends Actor {

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)
    case req@HttpRequest(spray.http.HttpMethods.GET, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.GET, None, req, path, sender)
    case req@HttpRequest(spray.http.HttpMethods.POST, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.POST, None, req, path, sender)
    case req@HttpRequest(spray.http.HttpMethods.PATCH, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.PATCH, None, req, path, sender)
    case req@HttpRequest(spray.http.HttpMethods.DELETE, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.DELETE, None, req, path, sender)
    case req@HttpRequest(spray.http.HttpMethods.PUT, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.PUT, None, req, path, sender)
    case req@HttpRequest(spray.http.HttpMethods.OPTIONS, Uri.Path(path), _, _, _) =>
      handlerRequest(yogi.server.OPTIONS, None, req, path, sender)
    case chunk@ChunkedRequestStart(r) =>
      r match {
        case req@HttpRequest(spray.http.HttpMethods.POST, Uri.Path(path), _, _, _) =>
          handlerRequest(yogi.server.POST, Some(chunk), req, path, sender)
        case req@HttpRequest(spray.http.HttpMethods.PATCH, Uri.Path(path), _, _, _) =>
          handlerRequest(yogi.server.PATCH, Some(chunk), req, path, sender)
        case req@HttpRequest(spray.http.HttpMethods.DELETE, Uri.Path(path), _, _, _) =>
          handlerRequest(yogi.server.DELETE, Some(chunk), req, path, sender)
        case req@HttpRequest(spray.http.HttpMethods.PUT, Uri.Path(path), _, _, _) =>
          handlerRequest(yogi.server.PUT, Some(chunk), req, path, sender)
        case req@HttpRequest(spray.http.HttpMethods.OPTIONS, Uri.Path(path), _, _, _) =>
          handlerRequest(yogi.server.OPTIONS, Some(chunk), req, path, sender)
      }
  }

  def handlerRequest(method: HttpMethod, chunk: Option[ChunkedRequestStart], request: HttpRequest, path: String, requestActor: ActorRef) {
    Request(method, path, chunk, request, requestActor, context)
    val routePath = server.routePaths.find { rp => path.startsWith(rp.path) }
    routePath match {
      case Some(a) =>
        val adjustedPath = path.replaceFirst(a.path, "")
        a.subServer.route(Request(method, adjustedPath, chunk, request, requestActor, context))
      case _ => requestActor ! HttpResponse(404)
    }
  }

}
