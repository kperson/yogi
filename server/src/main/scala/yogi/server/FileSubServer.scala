package yogi.server

import java.io.{FileInputStream, File}

import org.apache.commons.io.IOUtils

import spray.http.MediaTypes._
import spray.http.{HttpEntity, ContentType, MediaType, HttpResponse}


class FileSubServer(rootDirectory: File, index: Option[String] = Some("index.html"), cache: Boolean = false, mimeMap: Map[String, String] = FileSubServer.defaultMimeMap) extends SubServer {

  private val fileCache = scala.collection.mutable.Map[String, Array[Byte]]()

  get("/*") { req =>
    val adjustedPath = req.path.substring(1, req.path.length)
    fetch(adjustedPath) match {
      case Some(bytes) =>
        val mediaType = resolveContentType(adjustedPath).map(MediaType.custom(_)).getOrElse(`application/octet-stream`)
        val contentType = ContentType(mediaType)
        req.sender ! HttpResponse(entity = HttpEntity(contentType, bytes), status = 200)
      case _ => req.sender ! HttpResponse(404)
    }
  }

  private def resolveContentType(resourcePath: String) : Option[String] = mimeMap.get(suffix(resourcePath))

  private def suffix(path: String): String = path.reverse.takeWhile(_ != '.').reverse


  private def fetch(path: String): Option[Array[Byte]] = {
    val target = new File(rootDirectory, path)
    if(target.exists) {
      if (target.isDirectory) {
        index match {
          case Some(i) =>
            val f = new File(target, i)
            if(f.exists && f.isFile) {
              Some(read(f, path))
            }
            else {
              None
            }
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

  private def read(file: File, targetPath: String, checkCache: Boolean = cache) : Array[Byte] = {
    if(!checkCache) {
      val bytes = IOUtils.toByteArray(new FileInputStream(file))
      if(cache){
        fileCache.put(file.getAbsolutePath, bytes)
      }
      bytes
    }
    else {
      fileCache.get(file.getAbsolutePath) match {
        case Some(a) => a
        case _ => read(file, targetPath, false)
      }
    }
  }

}


object FileSubServer {

  lazy val defaultMimeMap:Map[String, String] =
    Map(
      "ai" -> "application/postscript",
      "aif" -> "audio/x-aiff",
      "aifc" -> "audio/x-aiff",
      "aiff" -> "audio/x-aiff",
      "apk" -> "application/vnd.android.package-archive",
      "asc" -> "text/plain",
      "asf" -> "video/x.ms.asf",
      "asx" -> "video/x.ms.asx",
      "au" -> "audio/basic",
      "avi" -> "video/x-msvideo",
      "bcpio" -> "application/x-bcpio",
      "bin" -> "application/octet-stream",
      "cab" -> "application/x-cabinet",
      "cdf" -> "application/x-netcdf",
      "class" -> "application/java-vm",
      "cpio" -> "application/x-cpio",
      "cpt" -> "application/mac-compactpro",
      "crt" -> "application/x-x509-ca-cert",
      "csh" -> "application/x-csh",
      "css" -> "text/css",
      "csv" -> "text/comma-separated-values",
      "dcr" -> "application/x-director",
      "dir" -> "application/x-director",
      "dll" -> "application/x-msdownload",
      "dms" -> "application/octet-stream",
      "doc" -> "application/msword",
      "dtd" -> "application/xml-dtd",
      "dvi" -> "application/x-dvi",
      "dxr" -> "application/x-director",
      "eps" -> "application/postscript",
      "etx" -> "text/x-setext",
      "exe" -> "application/octet-stream",
      "ez" -> "application/andrew-inset",
      "gif" -> "image/gif",
      "gtar" -> "application/x-gtar",
      "gz" -> "application/gzip",
      "gzip" -> "application/gzip",
      "hdf" -> "application/x-hdf",
      "hqx" -> "application/mac-binhex40",
      "htc" -> "text/x-component",
      "htm" -> "text/html",
      "html" -> "text/html",
      "ice" -> "x-conference/x-cooltalk",
      "ico" -> "image/x-icon",
      "ief" -> "image/ief",
      "iges" -> "model/iges",
      "igs" -> "model/iges",
      "jad" -> "text/vnd.sun.j2me.app-descriptor",
      "jar" -> "application/java-archive",
      "java" -> "text/plain",
      "jnlp" -> "application/x-java-jnlp-file",
      "jpe" -> "image/jpeg",
      "jpeg" -> "image/jpeg",
      "jpg" -> "image/jpeg",
      "js" -> "application/x-javascript",
      "json" -> "application/json",
      "jsp" -> "text/html",
      "kar" -> "audio/midi",
      "latex" -> "application/x-latex",
      "lha" -> "application/octet-stream",
      "lzh" -> "application/octet-stream",
      "man" -> "application/x-troff-man",
      "mathml" -> "application/mathml+xml",
      "me" -> "application/x-troff-me",
      "mesh" -> "model/mesh",
      "mid" -> "audio/midi",
      "midi" -> "audio/midi",
      "mif" -> "application/vnd.mif",
      "mol" -> "chemical/x-mdl-molfile",
      "mov" -> "video/quicktime",
      "movie" -> "video/x-sgi-movie",
      "mp2" -> "audio/mpeg",
      "mp3" -> "audio/mpeg",
      "mpe" -> "video/mpeg",
      "mpeg" -> "video/mpeg",
      "mpg" -> "video/mpeg",
      "mpga" -> "audio/mpeg",
      "ms" -> "application/x-troff-ms",
      "msh" -> "model/mesh",
      "msi" -> "application/octet-stream",
      "nc" -> "application/x-netcdf",
      "oda" -> "application/oda",
      "odb" -> "application/vnd.oasis.opendocument.database",
      "odc" -> "application/vnd.oasis.opendocument.chart",
      "odf" -> "application/vnd.oasis.opendocument.formula",
      "odg" -> "application/vnd.oasis.opendocument.graphics",
      "odi" -> "application/vnd.oasis.opendocument.image",
      "odm" -> "application/vnd.oasis.opendocument.text-master",
      "odp" -> "application/vnd.oasis.opendocument.presentation",
      "ods" -> "application/vnd.oasis.opendocument.spreadsheet",
      "odt" -> "application/vnd.oasis.opendocument.text",
      "ogg" -> "application/ogg",
      "otc" -> "application/vnd.oasis.opendocument.chart-template",
      "otf" -> "application/vnd.oasis.opendocument.formula-template",
      "otg" -> "application/vnd.oasis.opendocument.graphics-template",
      "oth" -> "application/vnd.oasis.opendocument.text-web",
      "oti" -> "application/vnd.oasis.opendocument.image-template",
      "otp" -> "application/vnd.oasis.opendocument.presentation-template",
      "ots" -> "application/vnd.oasis.opendocument.spreadsheet-template",
      "ott" -> "application/vnd.oasis.opendocument.text-template",
      "pbm" -> "image/x-portable-bitmap",
      "pdb" -> "chemical/x-pdb",
      "pdf" -> "application/pdf",
      "pgm" -> "image/x-portable-graymap",
      "pgn" -> "application/x-chess-pgn",
      "png" -> "image/png",
      "pnm" -> "image/x-portable-anymap",
      "ppm" -> "image/x-portable-pixmap",
      "pps" -> "application/vnd.ms-powerpoint",
      "ppt" -> "application/vnd.ms-powerpoint",
      "ps" -> "application/postscript",
      "qt" -> "video/quicktime",
      "ra" -> "audio/x-pn-realaudio",
      "ram" -> "audio/x-pn-realaudio",
      "ras" -> "image/x-cmu-raster",
      "rdf" -> "application/rdf+xml",
      "rgb" -> "image/x-rgb",
      "rm" -> "audio/x-pn-realaudio",
      "roff" -> "application/x-troff",
      "rpm" -> "application/x-rpm",
      "rtf" -> "application/rtf",
      "rtx" -> "text/richtext",
      "rv" -> "video/vnd.rn-realvideo",
      "ser" -> "application/java-serialized-object",
      "sgm" -> "text/sgml",
      "sgml" -> "text/sgml",
      "sh" -> "application/x-sh",
      "shar" -> "application/x-shar",
      "silo" -> "model/mesh",
      "sit" -> "application/x-stuffit",
      "skd" -> "application/x-koan",
      "skm" -> "application/x-koan",
      "skp" -> "application/x-koan",
      "skt" -> "application/x-koan",
      "smi" -> "application/smil",
      "smil" -> "application/smil",
      "snd" -> "audio/basic",
      "spl" -> "application/x-futuresplash",
      "src" -> "application/x-wais-source",
      "sv4cpio" -> "application/x-sv4cpio",
      "sv4crc" -> "application/x-sv4crc",
      "svg" -> "image/svg+xml",
      "swf" -> "application/x-shockwave-flash",
      "t" -> "application/x-troff",
      "tar" -> "application/x-tar",
      "tar.gz" -> "application/x-gtar",
      "tcl" -> "application/x-tcl",
      "tex" -> "application/x-tex",
      "texi" -> "application/x-texinfo",
      "texinfo" -> "application/x-texinfo",
      "tgz" -> "application/x-gtar",
      "tif" -> "image/tiff",
      "tiff" -> "image/tiff",
      "tr" -> "application/x-troff",
      "tsv" -> "text/tab-separated-values",
      "txt" -> "text/plain; charset=\"UTF-8",
      "ustar" -> "application/x-ustar",
      "vcd" -> "application/x-cdlink",
      "vrml" -> "model/vrml",
      "vxml" -> "application/voicexml+xml",
      "wav" -> "audio/x-wav",
      "webp" -> "image/webp",
      "wbmp" -> "image/vnd.wap.wbmp",
      "wml" -> "text/vnd.wap.wml",
      "wmlc" -> "application/vnd.wap.wmlc",
      "wmls" -> "text/vnd.wap.wmlscript",
      "wmlsc" -> "application/vnd.wap.wmlscriptc",
      "wrl" -> "model/vrml",
      "wtls-ca-certificate" -> "application/vnd.wap.wtls-ca-certificate",
      "xbm" -> "image/x-xbitmap",
      "xht" -> "application/xhtml+xml",
      "xhtml" -> "application/xhtml+xml",
      "xls" -> "application/vnd.ms-excel",
      "xml" -> "application/xml",
      "xpm" -> "image/x-xpixmap",
      "xsl" -> "application/xml",
      "xslt" -> "application/xslt+xml",
      "xul" -> "application/vnd.mozilla.xul+xml",
      "xwd" -> "image/x-xwindowdump",
      "xyz" -> "chemical/x-xyz",
      "z" -> "application/compress",
      "zip" -> "application/zip",
      "tff" -> "application/font-sfnt"
      )


}