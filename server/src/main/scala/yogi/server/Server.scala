package yogi.server

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.http.scaladsl.model.HttpMethods._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object Server {

  def defaultRecovery:PartialFunction[Throwable, Future[HttpResponse]] = {
    case _ => Future.successful(HttpResponse(500))
  }

}

class Server(interface: String, port: Int, recovery: PartialFunction[Throwable, Future[HttpResponse]] = Server.defaultRecovery)(implicit val system: ActorSystem, val materializer:Materializer) {

  case class RoutePath(path: String, subServer: ServerPath)

  private[yogi] val routePaths: ListBuffer[RoutePath] = ListBuffer.empty

  val serverHandler = new ServerHandler(this, recovery)(materializer.executionContext)

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
    val serverSource = Http().bind(interface = interface, port = port)(materializer)
    serverSource.to(Sink.foreach { connection =>
      connection.handleWithAsyncHandler(serverHandler.receive)
    }).run()(materializer)
  }

}

class ServerHandler[yogi](server: Server, recovery: PartialFunction[Throwable, Future[HttpResponse]])(implicit ec: ExecutionContext) {

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
    val path = if (request.getUri.path.startsWith("//")) request.getUri.path.substring(1, request.getUri.path.length) else request.getUri.path 
    val routePath = server.routePaths.find { rp => path.startsWith(rp.path) }
    routePath match {
      case Some(a) =>
        val adjustedPath = path.replaceFirst(a.path, "")
        Try(a.subServer.route(request, adjustedPath).recoverWith(recovery)) match {
          case Success(a) => a
          case Failure(t) => recovery(t)
        }
      case _ => Future.successful(HttpResponse(404))
    }
  }

}
