package yogi.server

trait HttpMethod

case object GETRequest extends HttpMethod
case object POSTRequest extends HttpMethod
case object DELETERequest extends HttpMethod
case object PUTRequest extends HttpMethod
case object PATCHRequest extends HttpMethod
case object OPTIONSRequest extends HttpMethod
