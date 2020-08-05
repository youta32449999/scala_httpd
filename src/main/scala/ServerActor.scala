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
      // リクエストを処理
      val request = new HttpRequest(socket.getInputStream)
      val path = request.getRequestPath()

      // レスポンスヘッダを返す
      val response = new HttpResponse(socket)
      response.writeLine("HTTP/1.1 200 OK")
      response.writeLine("Date: " + getDataStringUtc())
      response.writeLine("Server: Eseche/0.1")
      response.writeLine("Connection: close")
      response.writeLine("Content-type: text/html")
      response.writeLine("")

      // レスポンスボディを返す
      try {
        path.foreach(p => {
          val fis = new FileInputStream(DOCUMENT_ROOT + p)
          val content = readFile(fis)
          content.foreach(ch => response.write(ch))
        })
      } catch {
        case e: Exception => println(e)
      } finally {
        socket.close()
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
}