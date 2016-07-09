package yogi.server

import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Source, FileIO}

import java.io.File


case class CachedFile(mediaType: MediaType, file: File)

// A server path that serves data from the filesystem
class FileServerPath(rootDirectory: File, index: Option[String] = Some("index.html")) extends ServerPath {


  get("*").sync { (req, params) =>
    val path = params("splat").headOption.getOrElse("/").split("/").mkString(File.separator)
    val adjustedPath = if(path.startsWith("/")) path.substring(1, path.length) else path
    fetch(adjustedPath) match {
      case Some(data) =>
        val contentType = ContentType(data.mediaType, () => HttpCharsets.`UTF-8`)
        val source:Source[ChunkStreamPart, Any] = FileIO.fromFile(data.file).map(a => Chunk(a)) ++ Source(List(LastChunk))
        HttpResponse (entity = new Chunked(contentType, source), status = 200)
      case _ => HttpResponse(404)
    }
  }

  private def suffix(path: String): String = path.reverse.takeWhile(_ != '.').reverse

  private def fetch(path: String): Option[CachedFile] = {
    val target = new File(rootDirectory, path)
    if(target.exists) {
      if (target.isDirectory) {
        index match {
          case Some(i) =>
            val f = new File(target, i)
            val mediaType = MediaTypes.forExtension(suffix(f.getAbsolutePath))
            if(f.exists && f.isFile) Some(CachedFile(mediaType, f)) else None
          case _ => None
        }
      }
      else {
        val mediaType = MediaTypes.forExtension(suffix(target.getAbsolutePath))
        Some(CachedFile(mediaType, target))
      }
    }
    else {
      None
    }
  }

}