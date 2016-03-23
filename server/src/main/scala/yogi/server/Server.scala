package yogi.server

import akka.actor._
import akka.io.IO

import spray.can.Http
import spray.http.HttpMethods._
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
    case req@HttpRequest(GET, Uri.Path(path), _, _, _) =>
      handlerRequest(GETRequest, None, req, path, sender)
    case req@HttpRequest(POST, Uri.Path(path), _, _, _) =>
      handlerRequest(POSTRequest, None, req, path, sender)
    case req@HttpRequest(PATCH, Uri.Path(path), _, _, _) =>
      handlerRequest(PATCHRequest, None, req, path, sender)
    case req@HttpRequest(DELETE, Uri.Path(path), _, _, _) =>
      handlerRequest(DELETERequest, None, req, path, sender)
    case req@HttpRequest(PUT, Uri.Path(path), _, _, _) =>
      handlerRequest(PUTRequest, None, req, path, sender)
    case req@HttpRequest(OPTIONS, Uri.Path(path), _, _, _) =>
      handlerRequest(OPTIONSRequest, None, req, path, sender)
    case chunk@ChunkedRequestStart(r) =>
      r match {
        case req@HttpRequest(POST, Uri.Path(path), _, _, _) =>
          handlerRequest(POSTRequest, Some(chunk), req, path, sender)
        case req@HttpRequest(PATCH, Uri.Path(path), _, _, _) =>
          handlerRequest(PATCHRequest, Some(chunk), req, path, sender)
        case req@HttpRequest(DELETE, Uri.Path(path), _, _, _) =>
          handlerRequest(DELETERequest, Some(chunk), req, path, sender)
        case req@HttpRequest(PUT, Uri.Path(path), _, _, _) =>
          handlerRequest(PUTRequest, Some(chunk), req, path, sender)
        case req@HttpRequest(OPTIONS, Uri.Path(path), _, _, _) =>
          handlerRequest(OPTIONSRequest, Some(chunk), req, path, sender)
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
