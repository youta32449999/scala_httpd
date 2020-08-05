import java.io.OutputStream
import java.net.Socket

class HttpResponse(private val socket: Socket) {
  private val output = socket.getOutputStream

  // 1行の文字列を、バイト列としてOutputStreamに書き込む
  def writeLine(line: String): Unit = {
    line.toCharArray.toList
    val output = this.output
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

  def write(byte: Int): Unit = {
    val output = this.output
    output.write(byte)
  }
}
