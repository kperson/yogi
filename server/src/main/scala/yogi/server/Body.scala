package yogi.server

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpRequest

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


case class Body(request: HttpRequest)(implicit materializer:Materializer)  {

  /**
   * Reads the bytes from the request body
   */
  def futureAsBytes: Future[Array[Byte]] = {
    val buffer = ArrayBuffer[Byte]()
    request.entity.dataBytes
    val fetch = request.entity.dataBytes.runForeach { a => buffer.appendAll(a.toArray) }
    fetch.map { _ => buffer.toArray }(materializer.executionContext)
  }

  /**
   * Reads the request body as a string
   */
  def futureAsStr: Future[String] = futureAsBytes.map(new String(_))(materializer.executionContext)

}