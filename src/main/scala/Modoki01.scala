import java.io.{InputStream, OutputStream}
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

object Modoki01 extends App {
  private val DOCUMENT_ROOT = "./"

  val server = new ServerSocket(9999)
  val socket = server.accept()

  val input = socket.getInputStream()


  val line = readLine(input)

  println(line)


  // Requestからpathを取得する
  def getPath(input: InputStream): String = readLine(input) match {
    case None => ""
    case Some(line) => line match {
      case "" => ""
      case _ if(line.startsWith("GET")) => line.split(" ")(1)
      case _ => ""
    }
  }

  // InputStreamからのバイト列を、行単位で読み込むユーティリティメソッド
  def readLine(input: InputStream): Option[String] = {
    def pReadLine(input: InputStream, acc: String): String = input.read() match {
      case -1 => null
      case '\n' => acc
      case '\r' => pReadLine(input, acc)
      case ch =>
        pReadLine(input, acc + ch.toChar)
    }

    Option(pReadLine(input, ""))
  }

  // 1行の文字列を、バイト列としてOutputStreamに書き込む
  def writeLine(output: OutputStream, line: String): Unit = {
    line match {
      case "" => ()
      case _ =>
        output.write(line.head)
        writeLine(output, line.tail)
    }
  }

  // 現在時刻から、HTTP標準に合わせてフォーマットされた日付文字列を返す
  def getDataStringUtc(): String = {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US)

    df.setTimeZone(cal.getTimeZone())
    df.format(cal.getTime() + " GMT")
  }
}