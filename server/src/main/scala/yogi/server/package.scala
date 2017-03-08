package yogi

import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer

import org.scalatra.MultiParams


package object server {

  implicit class RequestExtensions(self: HttpRequest) {
      def body(implicit materializer:Materializer): Body = Body(self)
      def path: String = self.uri.path.toString
    }

  implicit class MultiParamsExtensions(self: MultiParams) {

    /**
     * Gets a single parameter from query string
     * @param str
     */
    def one(str: String): String = self(str).headOption.getOrElse("")


  }

}
