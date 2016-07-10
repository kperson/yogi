package yogi

import org.scalatra.MultiParams

package object server extends Request {

  implicit class MultiParamsExtensions(self: MultiParams) {

    /**
     * Gets a single parameter from query string
     * @param str
     */
    def one(str: String): String = self(str).head

  }

}
