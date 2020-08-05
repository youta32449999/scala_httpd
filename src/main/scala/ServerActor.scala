import java.io.{FileInputStream, OutputStream}
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.{Actor, ActorSystem, Props}

// ソケットの処理依頼
final case class Run(socket: Socket)

class ServerActor extends Actor {
  private val DOCUMENT_ROOT = "./public"

  override def receive: Receive = {
    case Run(socket: Socket) =>
      val request = new HttpRequest(socket.getInputStream)

      val path = request.getRequestPath()

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
        path.foreach(p => {
          val fis = new FileInputStream(DOCUMENT_ROOT + p)
          val content = readFile(fis)
          content.foreach(ch => output.write(ch))
          socket.close()
        })
      } catch {
        case e: Exception => println(e)
      }

    case msg => println(s"I cannot understand ${msg.toString}")
  }

  // ファイルの内容を読み込む
  def readFile(input: FileInputStream): Array[Int] = {
    def pReadFile(input: FileInputStream, content: Array[Int]): Array[Int] = input.read() match {
      case -1 => content
      case ch => pReadFile(input, content ++ Array(ch))
    }
    pReadFile(input, Array())
  }

  // 現在時刻から、HTTP標準に合わせてフォーマットされた日付文字列を返す
  def getDataStringUtc(): String = {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US)

    df.setTimeZone(cal.getTimeZone())
    df.format(cal.getTime()) + " GMT"
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
}