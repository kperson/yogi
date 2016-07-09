package yogi

import org.scalatra.MultiParams

package object server extends Request {

  implicit class MultiParamsExtensions(self: MultiParams) {

    def one(str: String): String = {
      self(str).head
    }

  }

}
