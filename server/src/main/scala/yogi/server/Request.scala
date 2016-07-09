package yogi.server

import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


trait Request {

  implicit class RequestExtensions(self: HttpRequest) {

    def body(implicit materializer:Materializer): Body = Body(self)
    def path: String = self.uri.path.toString

  }

}

case class Body(request: HttpRequest)(implicit materializer:Materializer)  {

  def futureAsBytes: Future[Array[Byte]] = {
    val buffer = ArrayBuffer[Byte]()
    request.entity.dataBytes
    val fetch = request.entity.dataBytes.runForeach { a => buffer.appendAll(a.toArray) }
    fetch.map { _ => buffer.toArray }(materializer.executionContext)
  }

  def futureAsStr: Future[String] = futureAsBytes.map(new String(_))(materializer.executionContext)

}