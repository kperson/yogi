package yogi.server


import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

import yogi.client.Request._


class RequestSpec extends YogiSpec with ScalaFutures {

  it should "submit a GET request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.get("/apple").raw(echoHandler)
    server.addRoute("/test", sub)
    val req = yogi.client.Request.get(s"${baseURL}/test/apple")
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("GET", "/apple", ""))
    }
  }

  it should "submit a POST request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.post("/apple").raw(echoHandler)
    server.addRoute(sub)
    val req = yogi.client.Request.post(s"${baseURL}/apple", body = "hello".getBytes)
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("POST", "/apple", "hello"))
    }
  }

  it should "submit a DELETE request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.delete("/apple").raw(echoHandler)
    server.addRoute(sub)
    val req = yogi.client.Request.delete(s"${baseURL}/apple", body = "hello".getBytes)
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("DELETE", "/apple", "hello"))
    }
  }

  it should "submit a PUT request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.put("/apple").raw(echoHandler)
    server.addRoute(sub)
    val req = yogi.client.Request.put(s"${baseURL}/apple", body = "hello".getBytes)
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("PUT", "/apple", "hello"))
    }
  }

  it should "submit an OPTIONS request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.options("/apple").raw(echoHandler)
    server.addRoute(sub)
    val req = yogi.client.Request.options(s"${baseURL}/apple")
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("OPTIONS", "/apple", ""))
    }
  }

  it should "submit an PATCH request" in withServer { (server, baseURL) =>
    val sub = new ServerPath()
    sub.patch("/apple").raw(echoHandler)
    server.addRoute(sub)
    val req = yogi.client.Request.patch(s"${baseURL}/apple")
    whenReady(req, 1.second) { res =>
      res.responseBody should be(echo("PATCH", "/apple", ""))
    }
  }

}
