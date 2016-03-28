package yogi.server

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpMethod, HttpResponse}
import org.scalatra._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class HandlerFilterSelector[A](path: String, serverPath: ServerPathWithContext[A])(implicit ec: ExecutionContext) extends HandlerSelector(path) {

  def async(asyncHandler: (HttpRequest, MultiParams, A) => Future[HttpResponse]) {

    requestHandler = Async { (request, params) =>
      val fetch = serverPath.fetchContext(request)
      fetch.flatMap {
        case Success(c) => asyncHandler(request, params, c)
        case Failure(ex) => serverPath.onContextFetchFailed(request, ex)
      }
    }
  }

  def sync(syncHandler: (HttpRequest, MultiParams, A) => HttpResponse) {

    requestHandler = Async { (request, params) =>
      val fetch = serverPath.fetchContext(request)
      fetch.flatMap {
        case Success(c) => Future.successful(syncHandler(request, params, c))
        case Failure(ex) => serverPath.onContextFetchFailed(request, ex)
      }
    }
  }

}

trait ServerPathWithContext[A] extends ServerPath {

  def fetchContext(request: HttpRequest) : Future[Try[A]]
  def onContextFetchFailed(request: HttpRequest, ex: Throwable) : Future[HttpResponse] = Future.successful(HttpResponse(400))

  def getWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, GET)
    selector
  }

  def postWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, POST)
    selector
  }

  def deleteWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, DELETE)
    selector
  }

  def putWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, PUT)
    selector
  }

  def patchWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, PATCH)
    selector
  }

  def optionsWithContext(path: String)(implicit ec: ExecutionContext): HandlerFilterSelector[A] = {
    val selector = new HandlerFilterSelector[A](path, this)
    addContextHandler(path, selector, OPTIONS)
    selector
  }

  private def addContextHandler(path: String, handler: HandlerFilterSelector[A], method: HttpMethod) {
    addHandler(path, handler, method)
  }

}