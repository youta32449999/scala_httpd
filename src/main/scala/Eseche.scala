import java.io.{FileInputStream, InputStream, OutputStream}
import java.net.{ServerSocket, Socket}
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.{ActorSystem, Props}

import ServerActorMessage.Run

object Eseche extends App {
  private val DOCUMENT_ROOT = "./public"

  val server = new ServerSocket(9999)
  val system = ActorSystem("system")
  val serverActor = system.actorOf(Props[ServerActor], "serverActor")

  while(true){
    val socket = server.accept()
    serverActor ! Run(socket)
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