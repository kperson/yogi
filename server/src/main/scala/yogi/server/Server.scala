package yogi.server

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.http.scaladsl.model.HttpMethods._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


class Server(interface: String, port: Int)(implicit val system: ActorSystem, val materializer:Materializer) {

  case class RoutePath(path: String, subServer: ServerPath)

  private[yogi] val routePaths: ListBuffer[RoutePath] = ListBuffer.empty
  val serverHandler = new ServerHandler(this)

  /**
   * Adds a sub server contain to a path
   *
   * @param path the containing path
   * @param subServer the sub server
   */
  def addRoute(path: String, subServer: ServerPath) {
    routePaths.append(RoutePath(path, subServer))
  }

  /**
   * Adds a sub server to the root
   *
   * @param subServer the sub server
   */
  def addRoute(subServer: ServerPath) {
    routePaths.append(RoutePath("", subServer))
  }

  /**
   * Starts the server
   */
  def start(): Future[Http.ServerBinding] =  {
    val serverSource = Http().bind(interface = interface, port = port)
    serverSource.to(Sink.foreach { connection =>
      connection.handleWithAsyncHandler(serverHandler.receive)
    }).run()
  }

}

class ServerHandler[yogi](server: Server) {

  def receive(request: HttpRequest) : Future[HttpResponse] = {
    request match {
      case req@HttpRequest(GET, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
      case req@HttpRequest(POST, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
      case req@HttpRequest(PATCH, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
      case req@HttpRequest(DELETE, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
      case req@HttpRequest(PUT, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
      case req@HttpRequest(OPTIONS, Uri.Path(path), _, _, _) =>
        handlerRequest(req)
    }
  }

  def handlerRequest(request: HttpRequest): Future[HttpResponse] = {
    val path = request.getUri.path
    val routePath = server.routePaths.find { rp => path.startsWith(rp.path) }
    routePath match {
      case Some(a) =>
        val adjustedPath = path.replaceFirst(a.path, "")
        a.subServer.route(request, adjustedPath)
      case _ => Future.successful(HttpResponse(404))
    }
  }

}
