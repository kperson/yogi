package yogi.demo

import akka.http.scaladsl.model.HttpResponse

import yogi.server.ServerPath


class PingPong extends ServerPath {

  get("/ping").sync { (request, f) =>
    HttpResponse(200, entity = "pong")
  }

  get("/pong").sync { (request, f) =>
    HttpResponse(200, entity = "ping")
  }

}