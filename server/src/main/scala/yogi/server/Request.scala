package yogi.server

import akka.actor.{Actor, Props, ActorContext, ActorRef}

import org.scalatra.MultiParams
import org.scalatra.util.MultiMap

import spray.can.Http.RegisterChunkHandler
import spray.http._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Promise, Future}

case class Body(chunkStart: Option[ChunkedRequestStart], context: ActorContext, sender: ActorRef, sprayRequest: HttpRequest) {

  import context.dispatcher

  def futureAsBytes: Future[Array[Byte]] = {
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

  def futureAsStr: Future[String] = futureAsBytes.map(new String(_))

  def bodyAsStream: Stream[Byte] = sprayRequest.asPartStream().flatMap {
    case c: MessageChunk => Some(c.data.toByteArray)
    case _ => None
  }.flatten
}
case class Request(method: HttpMethod, path: String, chunkStart: Option[ChunkedRequestStart], sprayRequest: HttpRequest, sender: ActorRef, context: ActorContext, params: MultiParams = MultiMap(Map.empty)) {

  def header(name: String) : Option[String] = sprayRequest.headers.find(_.lowercaseName == name.toLowerCase).map(_.value)

  def body = Body(chunkStart, context, sender, sprayRequest)

}

class ByteReader(promise: Promise[Array[Byte]]) extends Actor {

  var buffer = ArrayBuffer[Byte]()

  def receive = {
    case c: MessageChunk =>
      c.data.toByteArray.foreach(buffer.append(_))
    case e: ChunkedMessageEnd =>
      promise.success(buffer.toArray)
  }

}