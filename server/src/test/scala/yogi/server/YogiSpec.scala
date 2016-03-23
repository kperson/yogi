package yogi.server

import akka.actor.ActorSystem

import java.net.ServerSocket

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}

import spray.http.HttpResponse

import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global


trait YogiSpec extends FlatSpec with ScalaFutures with Matchers {

  val r = new Random(31)

  def echoHandler(request: Request) {
    val methodName = request.method match {
      case GETRequest => "GET"
      case POSTRequest => "POST"
      case PUTRequest => "PUT"
      case DELETERequest => "DELETE"
      case PATCHRequest => "PATCH"
      case OPTIONSRequest => "OPTIONS"
    }
    request.bodyAsBytes.onSuccess {
      case fetchedBytes =>
        request.sender ! HttpResponse(200, echo(methodName, request.path, new String(fetchedBytes)))
    }

  }

  def echo(method: String, path: String, body: String) = {
    s"METHOD:${method}\nPATH:${path}\nBODY:${body}"
  }

  def withServer(testCode: (Server, String) => Any): Unit = {
    implicit val system = ActorSystem(r.nextInt(1000000).toString)
    val socket = new ServerSocket(0)
    val openPort = socket.getLocalPort
    socket.close()
    val server = new Server("localhost", openPort)
    server.start()
    Thread.sleep(500)
    testCode(server, s"http://localhost:${openPort}")
    server.stop()
    system.shutdown()
  }

  implicit def toTestTimeout(duration: FiniteDuration) = Timeout(Span(duration.toMillis, Millis))

}
