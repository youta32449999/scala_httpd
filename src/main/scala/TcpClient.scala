import java.io.{FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.net.Socket

object TcpClient extends App {
  try {
    val socket = new Socket("localhost", 8888)
    val fis = new FileInputStream("client_send.txt")
    val fos = new FileOutputStream("client_recv.txt")

    // client_send.txtの内容をサーバーに送信
    val output = socket.getOutputStream()
    sendFile(output, fis)

    // サーバーからの返信をclient_recv.txtに出力
    val input = socket.getInputStream()
    receiveFile(input, fos)

  } catch {
    case e: Exception => println(e)
  }

  def sendFile(output: OutputStream, fis: FileInputStream): Unit = fis.read() match {
    case -1 => output.write(0)
    case ch =>
      output.write(ch)
      sendFile(output, fis)
  }

  def receiveFile(input: InputStream, fos: FileOutputStream): Unit = input.read() match {
    case -1 =>
    case ch =>
      fos.write(ch)
      receiveFile(input, fos)
  }
}