package yogi.server

import java.io.{FileInputStream, File}

import org.apache.commons.io.IOUtils

import scala.concurrent.duration._

import yogi.client.Request._

class FileServerPathSpec extends YogiSpec {

  def withStaticServer(testCode: (Server, String, File) => Any) {
    withServer { (server, baseURL) =>
      implicit val m = server.materializer
      val staticDirectory = new File(getClass.getResource("/static-test").getFile)
      val sub = new FileServerPath(staticDirectory)
      server.addRoute(sub)
      testCode(server, baseURL, staticDirectory)
    }
  }

  it should "fetch a file" in withStaticServer { (server, baseURL, staticDirectory) =>
    val req = yogi.client.Request.get(s"${baseURL}/hello.txt")
    whenReady(req, 1.second) { res =>
      val helloContent = IOUtils.toByteArray(new FileInputStream(new File(staticDirectory, "hello.txt")))
      res.responseBytes should be (helloContent)
    }
  }

  it should "default to index" in withStaticServer { (server, baseURL, staticDirectory) =>
    val req = yogi.client.Request.get(s"${baseURL}")
    whenReady(req, 1.second) { res =>
      val helloContent = IOUtils.toByteArray(new FileInputStream(new File(staticDirectory, "index.html")))
      res.responseBytes should be (helloContent)
    }
  }


  it should "default 404 if a file does not exist" in withStaticServer { (server, baseURL, staticDirectory) =>
    val fileReq = yogi.client.Request.get(s"${baseURL}/world.txt")
    whenReady(fileReq, 1.second) { res =>
      res.statusCode should be (404)
    }
  }

  it should "default 404 if a directory index does not exist" in withStaticServer { (server, baseURL, staticDirectory) =>
    val dirReq = yogi.client.Request.get(s"${baseURL}/dir")
    whenReady(dirReq, 1.second) { res =>
      res.statusCode should be (404)
    }
  }

}
