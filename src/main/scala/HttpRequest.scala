import java.io.InputStream

/**
  * HttpRequestに関する処理を行う
  * @param input InputStream
  */
class HttpRequest(input: InputStream) {

  private val header = readHeader(input)
  val parsedHeader = parseHeader(header)

  def getRequestMethod(): Option[String] = {
    parsedHeader.get("Method")
  }

  def getRequestPath(): Option[String] = {
    parsedHeader.get("Path")
  }

  // ヘッダーの内容をMapにして扱いやすくする
  private def parseHeader(header: Option[String]): Map[String, String] = {
    this.header.map(h => {
      val lines = h.split("\r\n")
      // リクエストラインの取得
      val requestLines = lines(0).split(" ")
      val requestLineMap = Map(
        "Method" -> requestLines(0).trim,
        "Path" -> requestLines(1).trim,
        "Protocol" -> requestLines(2).trim
      )

      // ヘッダーの取得
      val headerBodyMap = lines.tail.map(h => {
        val headerLine = h.split(":", 2)
        (headerLine(0).trim, headerLine(1).trim)
      }).toMap

      requestLineMap ++ headerBodyMap
    }) match {
      case None => Map()
      case Some(m) => m
    }
  }

  // リクエストされているリソースのパスを返す(メソッド, パス, プロトコル)
  private def getRequestLine(): (Option[String], Option[String], Option[String]) = {
    val requestLine = this.header match {
      case None => (None, None, None)
      case Some(header) =>
        val requestLine = header.split("\r\n")(0).split(" ")
        (Option(requestLine(0)), Option(requestLine(1)), Option(requestLine(2)))
    }
    requestLine
  }

  // リクエストライン+リクエストヘッダは空行まで
  private def readHeader(input: InputStream): Option[String] = {
    def pReadHeader(input: InputStream, header: String): String = readLine(input) match {
      case None => null
      case Some("\r\n") => header
      case Some(line) => pReadHeader(input, header + line)
    }
    Option(pReadHeader(input, ""))
  }

  // InputStreamから1行ずつ読み込む
  private def readLine(input: InputStream): Option[String] = {
    def pReadLine(input: InputStream, line: String): String = input.read match {
      case -1 => null
      case '\n' => line + '\n'.toChar
      case ch => pReadLine(input, line + ch.toChar)
    }
    Option(pReadLine(input, ""))
  }
}
