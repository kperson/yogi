package yogi.client

import com.ning.http.client.{RequestBuilder, Response}
import dispatch._
import Defaults._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

object Request {

  def get(url: String, params: Map[String, String] = Map.empty, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "GET", headers)
    params.foreach { case (k, v) =>
      builder.addQueryParameter(k, v)
    }
    Http(builder)
  }


  def postWithPost(url: String, params: Map[String, Seq[String]] = Map.empty, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "POST", headers)
    builder.setParameters(params)
    Http(builder)
  }

  def putWithParams(url: String, params: Map[String, Seq[String]] = Map.empty, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "PUT", headers)
    builder.setParameters(params)
    Http(builder)
  }

  def deleteWithParams(url: String, params: Map[String, Seq[String]] = Map.empty, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "DELETE", headers)
    builder.setParameters(params)
    Http(builder)
  }

  def patchWithParams(url: String, params: Map[String, Seq[String]] = Map.empty, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "PATCH", headers)
    builder.setParameters(params)
    Http(builder)
  }


  def post(url: String, body: Array[Byte] = Array.emptyByteArray, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None): Future[Response] = {
    val builder = createRequestBuilder(url, "POST", headers)
    builder.setBody(body)
    Http(builder)
  }

  def put(url: String, body: Array[Byte] = Array.emptyByteArray, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "PUT", headers)
    builder.setBody(body)
    Http(builder)
  }

  def delete(url: String, body: Array[Byte] = Array.emptyByteArray, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "DELETE", headers)
    builder.setBody(body)
    Http(builder)
  }

  def patch(url: String, body: Array[Byte] = Array.emptyByteArray, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "PATCH", headers)
    builder.setBody(body)
    Http(builder)
  }

  def options(url: String, headers: Map[String, String] = Map.empty, requestTimeout: Option[FiniteDuration] = None) : Future[Response] = {
    val builder = createRequestBuilder(url, "OPTIONS", headers)
    Http(builder)
  }


  private def createRequestBuilder(url: String, methodName: String, headers: Map[String, String], requestTimeout: Option[FiniteDuration] = None): RequestBuilder = {
    val builder = new RequestBuilder(methodName).setUrl(url)
    headers.foreach { case (k, v) =>
      builder.addHeader(k, v)
    }
    requestTimeout.foreach { a =>
      builder.setRequestTimeout(a.toMillis.toInt)
    }
    builder
  }

  implicit class RichResponse(self: Response) {

    def statusCode: Int = self.getStatusCode
    def responseBody: String = self.getResponseBody
    def header(name: String) : String = self.getHeader(name)
    def headers(name: String) : List[String] = self.getHeaders(name).asScala.toList
    def responseStream = self.getResponseBodyAsStream
    def responseBytes = self.getResponseBodyAsBytes

  }

}
