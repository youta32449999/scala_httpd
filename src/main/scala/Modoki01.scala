import java.io.{FileInputStream, InputStream, OutputStream}
import java.net.{ServerSocket, Socket}
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import scala.util.{Failure, Success, Try}

object Modoki01 extends App {
  private val DOCUMENT_ROOT = "./public"

  val server = new ServerSocket(9999)
  val socket = server.accept()

  val input = socket.getInputStream()


  val path = getPath(input)
  println(path)

  val output = socket.getOutputStream()

  // レスポンスヘッダを返す
  writeLine(output, "HTTP/1.1 200 OK")
  writeLine(output, "Date: " + getDataStringUtc())
  writeLine(output, "Server: Modoki/0.1")
  writeLine(output, "Connection: close")
  writeLine(output, "Content-type: text/html")
  writeLine(output, "")

  // レスポンスボディを返す
  try {
    val fis = new FileInputStream(DOCUMENT_ROOT + path)
    val content = readFile(fis)
    println(content)
    content.foreach(ch => output.write(ch))

    socket.close()
  } catch {
    case e: Exception => println(e)
  }

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
    line.toCharArray.toList
    def pWriteLine(output: OutputStream, bytes: List[Char]): Unit = {
      bytes match {
        case Nil =>
          output.write('\r')
          output.write('\n')
        case x::xs =>
          output.write(x)
          pWriteLine(output, xs)
      }
    }
    pWriteLine(output, line.toCharArray.toList)
  }

  // 現在時刻から、HTTP標準に合わせてフォーマットされた日付文字列を返す
  def getDataStringUtc(): String = {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US)

    df.setTimeZone(cal.getTimeZone())
    df.format(cal.getTime()) + " GMT"
  }

  // ファイルの内容を読み込む
  def readFile(input: FileInputStream): Array[Int] = {
    def pReadFile(input: FileInputStream, content: Array[Int]): Array[Int] = input.read() match {
      case -1 => content
      case ch => pReadFile(input, content ++ Array(ch))
    }
    pReadFile(input, Array())
  }
}