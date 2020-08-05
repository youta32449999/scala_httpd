import java.io.InputStream

/**
  * HttpRequestに関する処理を行う
  * @param input InputStream
  */
class HttpRequest(input: InputStream) {

  private val header = readHeader(input)
  val (_method, _path, _proto) = getRequestLine()

  private val method = _method
  private val path = _path
  private val proto = _proto

  def getRequestMethod(): Option[String] = {
    this.method
  }

  def getRequestPath(): Option[String] = {
    this.path
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
