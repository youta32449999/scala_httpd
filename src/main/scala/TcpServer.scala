import java.io.{FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.net.ServerSocket

object TcpServer extends App {
  try {
    val server = new ServerSocket(8888)
    val fos = new FileOutputStream("server_recv.txt")
    val fis = new FileInputStream("server_send.txt")

    println("クライアントからの接続を待ちます")
    val socket = server.accept()
    println("クライアント接続")

    // クライアントから受け取った内容をserver_recv.txtに出力
    val input = socket.getInputStream()
    writeFileFromStream(input,fos)

    // server_send.txtの内容をクライアントに送付
    val output = socket.getOutputStream()
    sendFileFromStream(output, fis)

    // 接続を閉じる
    socket.close()
    println("通信を終了しました")

  } catch {
    case e: Exception => println(e)
  }

  // クライアントは終了のマークとして0を送付してくる
  def writeFileFromStream(input: InputStream, fileOutputStream: FileOutputStream): Unit = input.read() match {
    case 0 =>
    case ch =>
      fileOutputStream.write(ch)
      writeFileFromStream(input, fileOutputStream)
  }

  // クライアントにFileInputStreamの内容を送付
  def sendFileFromStream(output: OutputStream, fileInputStream: FileInputStream): Unit = fileInputStream.read() match {
    case -1 =>
    case ch =>
      output.write(ch)
      sendFileFromStream(output, fileInputStream)
  }
}