package yogi.server

import akka.actor._
import akka.io.IO

import spray.can.Http
import spray.http.{HttpResponse, ChunkedRequestStart, Uri, HttpRequest}
import scala.collection.mutable.ListBuffer


class Server(interface: String, port: Int)(implicit val system: ActorSystem) {

  case class RoutePath(path: String, subServer: SubServer)

  private[yogi] val routePaths: ListBuffer[RoutePath] = ListBuffer.empty
  private var handler: Option[ActorRef] = None


  def addRoute(path: String, subServer: SubServer) {
    routePaths.prepend(RoutePath(path, subServer))
  }

  def addRoute(subServer: SubServer) {
    routePaths.append(RoutePath("", subServer))
  }

  def start() {
    val h = system.actorOf(Props(new ServerHandler(this)))
    IO(Http) ! Http.Bind(h, interface = interface, port = port)
    handler = Some(h)
  }

  def stop() {
    handler.foreach(system.stop(_))
  }

}

class ServerHandler(server: Server) extends Actor {

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
        a.subServer.handle(Request(method, adjustedPath, chunk, request, requestActor, context))
      case _ => send404(requestActor)
    }
  }

  private def send404(client: ActorRef) {
    client ! HttpResponse(404)
  }

}
