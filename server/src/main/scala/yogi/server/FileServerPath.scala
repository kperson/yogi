package yogi.server


import akka.http.scaladsl.model._

import java.io.{InputStreamReader, FileInputStream, File}

import org.apache.commons.io.IOUtils


case class CachedFile(encoding: String, mediaType: MediaType, bytes: Array[Byte])

// A server path that serves data from the filesystem
class FileServerPath(rootDirectory: File, index: Option[String] = Some("index.html"), cache: Boolean = false) extends ServerPath {

  private val fileCache = scala.collection.mutable.Map[String, CachedFile]()

  get("/*").sync { (req, params) =>
    val adjustedPath = params("splat").mkString(File.separator)
    fetch(adjustedPath) match {
      case Some(data) =>
        val contentType = ContentType(data.mediaType, () => HttpCharset.custom(data.encoding))
        HttpResponse(entity = HttpEntity(contentType, data.bytes), status = 200)
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
            if(f.exists && f.isFile) Some(read(f, path)) else None
          case _ => None
        }
      }
      else {
        Some(read(target, path))
      }
    }
    else {
      None
    }
  }

  private def read(file: File, targetPath: String, checkCache: Boolean = cache) : CachedFile = {
    if(!checkCache) {
      val is = new FileInputStream(file)
      val r =  new InputStreamReader(is)
      val encoding = r.getEncoding
      val bytes = IOUtils.toByteArray(r)
      val mediaType = MediaTypes.forExtension(suffix(file.getAbsolutePath))
      val cached = CachedFile(encoding, mediaType, bytes)
      if(cache){
        fileCache.put(file.getAbsolutePath, cached)
      }
      cached
    }
    else {
      fileCache.get(file.getAbsolutePath) match {
        case Some(a) => a
        case _ => read(file, targetPath, false)
      }
    }
  }

}