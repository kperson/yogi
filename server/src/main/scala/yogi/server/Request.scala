package yogi.server

import akka.actor.{Actor, Props, ActorContext, ActorRef}

import spray.can.Http.RegisterChunkHandler
import spray.http._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Promise, Future}


case class Request(method: HttpMethod, path: String, chunkStart: Option[ChunkedRequestStart], sprayRequest: HttpRequest, sender: ActorRef, context: ActorContext) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def header(name: String) : Option[String] = {
    sprayRequest.headers.find(_.lowercaseName == name.toLowerCase).map(_.value)
  }

  def bodyAsBytes: Future[Array[Byte]] = {
    val promise = Promise[Array[Byte]]()
    chunkStart match {
      case Some(start) =>
        val handler = context.actorOf(Props(new ByteReader(promise)))
        sender ! RegisterChunkHandler(handler)
      case _ =>
        val handler = context.actorOf(Props(new ByteReader(promise)))
        sprayRequest.asPartStream().foreach(handler ! _)
    }
    promise.future
  }

  def bodyAsString: Future[String] = bodyAsString.map(new String(_))

  private class ByteReader(promise: Promise[Array[Byte]]) extends Actor {

    var buffer = ArrayBuffer[Byte]()

    def receive = {
      case c: MessageChunk =>
        c.data.toByteArray.foreach(buffer.append(_))
      case e: ChunkedMessageEnd =>
        promise.success(buffer.toArray)
    }

  }

}
