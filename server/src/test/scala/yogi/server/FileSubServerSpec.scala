package yogi.server

import java.io.{FileInputStream, File}

import org.apache.commons.io.IOUtils

import scala.concurrent.duration._

import yogi.client.Request._

class FileSubServerSpec extends YogiSpec {

  it should "fetch a file" in withServer { (server, baseURL) =>
    val staticDirectory = new File(getClass.getResource("/static-test").getFile)
    val sub = new FileServerPath(staticDirectory)
    server.addRoute(sub)
    val req = yogi.client.Request.get(s"${baseURL}/hello.txt")
    whenReady(req, 1.second) { res =>
      val helloContent = IOUtils.toByteArray(new FileInputStream(new File(staticDirectory, "hello.txt")))
      res.responseBytes should be (helloContent)
    }
  }

  it should "default to index" in withServer { (server, baseURL) =>
    val staticDirectory = new File(getClass.getResource("/static-test").getFile)
    val sub = new FileServerPath(staticDirectory)
    server.addRoute(sub)
    val req = yogi.client.Request.get(s"${baseURL}")
    whenReady(req, 1.second) { res =>
      val helloContent = IOUtils.toByteArray(new FileInputStream(new File(staticDirectory, "index.html")))
      res.responseBytes should be (helloContent)
    }
  }

  it should "default 404 if a file does not exist" in withServer { (server, baseURL) =>
    val staticDirectory = new File(getClass.getResource("/static-test").getFile)
    val sub = new FileServerPath(staticDirectory)
    server.addRoute(sub)
    val req = yogi.client.Request.get(s"${baseURL}/world.txt")
    whenReady(req, 1.second) { res =>
      res.statusCode should be (404)
    }
  }

  it should "default 404 if a directory index does not exist" in withServer { (server, baseURL) =>
    val staticDirectory = new File(getClass.getResource("/static-test").getFile)
    val sub = new FileServerPath(staticDirectory)
    server.addRoute(sub)
    val req = yogi.client.Request.get(s"${baseURL}/dir")
    whenReady(req, 1.second) { res =>
      res.statusCode should be (404)
    }
  }

}
