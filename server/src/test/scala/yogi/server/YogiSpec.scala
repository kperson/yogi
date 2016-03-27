package yogi.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.HttpMethods._

import java.net.ServerSocket

import akka.stream.{Materializer, ActorMaterializer}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatra.MultiParams

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random


trait YogiSpec extends FlatSpec with ScalaFutures with Matchers {

  val r = new Random(31)

  def echoHandler(request: HttpRequest, params: MultiParams)(implicit materializer:Materializer) : Future[HttpResponse] = {

    import materializer.executionContext
    val methodName = request.method match {
      case GET => "GET"
      case POST => "POST"
      case PUT => "PUT"
      case DELETE => "DELETE"
      case PATCH => "PATCH"
      case OPTIONS => "OPTIONS"
    }
    request.body.futureAsStr.map { str => HttpResponse(200, entity = echo(methodName, request.path, str)) }

  }

  def echo(method: String, path: String, body: String) = {
    s"METHOD:${method}\nPATH:${path}\nBODY:${body}"
  }

  def withServer(testCode: (Server, String) => Any): Unit = {
    implicit val system = ActorSystem(r.nextInt(1000000).toString)
    implicit val materializer = ActorMaterializer()
    val socket = new ServerSocket(0)
    val openPort = socket.getLocalPort
    socket.close()
    val server = new Server("localhost", openPort)
    val bindFetch = server.start()

    val bind = Await.result(bindFetch, 3.seconds)
    testCode(server, s"http://localhost:${openPort}")
    bind.unbind()
    system.shutdown()
  }

  implicit def toTestTimeout(duration: FiniteDuration) = Timeout(Span(duration.toMillis, Millis))

}
