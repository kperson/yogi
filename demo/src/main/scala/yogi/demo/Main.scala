package yogi.demo

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import yogi.server.{FileServerPath, Server}


object Main extends App {

  implicit val system = ActorSystem("demo")
  implicit val materializer = ActorMaterializer()

  val server =  new Server("localhost", args(0).toInt)
  server.addRoute("/pp", new PingPong)

  val staticDirectory = new File(getClass.getResource("/static").getFile)
  server.addRoute("/static", new FileServerPath(staticDirectory))
  server.start()
}
