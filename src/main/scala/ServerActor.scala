import java.io.{FileInputStream, FileOutputStream, OutputStream}
import java.net.Socket
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.Actor
import akka.util.ReentrantGuard

import scala.util.matching.Regex

object ServerActorMessage {
  // ソケットの処理依頼
  final case class Run(socket: Socket)
}

class ServerActor extends Actor {
  import ServerActorMessage.Run

  private val DOCUMENT_ROOT = "./public"

  override def receive: Receive = {
    case Run(socket: Socket) =>
      // リクエストを処理
      val request = new HttpRequest(socket.getInputStream)
      request.getRequestMethod.map(method => method match {
        case "GET" =>
          request.getRequestPath.foreach(path => {
            // ファイルを返す
            try {
                val fis = new FileInputStream(DOCUMENT_ROOT + path)
                val content = readFile(fis)

                // レスポンスヘッダを返す
                val response = new HttpResponse(socket)
                response.writeLine("HTTP/1.1 200 OK")
                response.writeLine("Date: " + getDataStringUtc())
                response.writeLine("Server: Eseche/0.1")
                response.writeLine("Connection: close")
                response.writeLine("Content-type: text/html")
                response.writeLine(s"Content-Length: ${content.length}")
                response.writeLine("")
                // レスポンスボディを返す
                content.foreach(ch => response.write(ch))

            } catch {
              case e: Exception =>
                println(e)
                // 404を返す
                val response = new HttpResponse(socket)
                response.writeLine("HTTP/1.1 404 NOT FOUND")
                response.writeLine("Date: " + getDataStringUtc())
                response.writeLine("Server: Eseche/0.1")
                response.writeLine("Connection: close")
                response.writeLine("Content-type: text/html")
                response.writeLine(s"Content-Length: 0")
                response.writeLine("")
            } finally {
              socket.close()
            }

          })

        case "POST" =>
          // multipart/form-dataの境界を取得
          request.parsedHeader.get("Content-Type").foreach(contentType => {
            if(contentType.startsWith("multipart/form-data")){
              val p = "^.+boundary=(.+)$".r
              contentType match {
                case p(b) =>
                  val boundary = "--" + b.trim
                  request.parsedHeader.get("Content-Length").foreach(contentLength => {
                    val _POST = parseMultiPartFormData(socket, "\r\n" + boundary + "\r\n", contentLength.toInt)
                    _POST.foreach(m => {
                      println(m._1)
                      m._2 match {
                        case v: Array[Any] => println(v.toList)
                        case v: Any => println(v)
                      }
                    })
                  })

                case _ => println("match error")
              }
            }
          })

          request.getRequestPath.foreach(path => {
            // ファイルを返す
            try {
              val fis = new FileInputStream(DOCUMENT_ROOT + path)
              val content = readFile(fis)

              // レスポンスヘッダを返す
              val response = new HttpResponse(socket)
              response.writeLine("HTTP/1.1 200 OK")
              response.writeLine("Date: " + getDataStringUtc())
              response.writeLine("Server: Eseche/0.1")
              response.writeLine("Connection: close")
              response.writeLine("Content-type: text/html")
              response.writeLine(s"Content-Length: ${content.length}")
              response.writeLine("")
              // レスポンスボディを返す
              content.foreach(ch => response.write(ch))

            } catch {
              case e: Exception =>
                println(e)
                // 404を返す
                val response = new HttpResponse(socket)
                response.writeLine("HTTP/1.1 404 NOT FOUND")
                response.writeLine("Date: " + getDataStringUtc())
                response.writeLine("Server: Eseche/0.1")
                response.writeLine("Connection: close")
                response.writeLine("Content-type: text/html")
                response.writeLine(s"Content-Length: 0")
                response.writeLine("")
            } finally {
              socket.close()
            }

          })
      })

    case msg => println(s"I cannot understand ${msg.toString}")
  }


  def parseMultiPartFormData(socket: Socket, boundary: String, contentLength: Int): Map[String, Any] = {

    def pParseMultiPartFormData(post: Map[String, Any], readBytes: Int): Map[String, Any] = {
      // Content-Length分だけ読み込んだら終了
      if(readBytes >= contentLength) {
        post
      } else {
        // Content-Lengthに達しない場合は読み込みを続ける
        scala.io.Source.fromBytes(readLine(socket)).mkString match {

          case line if (line.startsWith("Content-Disposition:")) =>
            val p1 = "^.*name=\"(.*)\"\r\n$".r
            val p2 = "^.*name=\"(.*)\".*filename=\"(.*)\"\r\n$".r
            line match {
              // fileが添付されている場合
              case p2(name, filename) =>
                println(s"name is $name filename is $filename")
                // Content-Typeがあるので読み込み
                val contentType = readLine(socket)
                // 仕様により空白行があるので読み飛ばし
                val emptyLine = readLine(socket)

                // boundaryまで読み込む
                val content = readLine(socket, boundary.trim)

                // tmp領域にファイルを書き込む
                val tmpDir = Paths.get("./tmp")
                if(Files.notExists(tmpDir))Files.createDirectory(tmpDir)
                val filepath = s"./tmp/$filename"
                if(filename != "") {
                  println(filepath)
                  val fos = new FileOutputStream(filepath)
                  content.slice(0, content.length - boundary.getBytes().length).foreach(byte => {
                    fos.write(byte)
                  })
                }

                // 読み込みByte数を計算
                val nextReadBytes = readBytes + line.getBytes().length + contentType.length + emptyLine.length + content.length

                // postを更新する
                val nextPost = post ++ Map(name -> Array(filename, filepath))

                // 読み込みを継続する
                pParseMultiPartFormData(nextPost, nextReadBytes)

              // key-valueの場合
              case p1(name) =>
                // 仕様により空白行があるので読み飛ばし
                val emptyLine = readLine(socket)

                // boundaryまで読み込む
                val content = readLine(socket, boundary)
                val value = scala.io.Source.fromBytes(content).mkString.replace(boundary, "")

                // Mapに情報を格納して次へ
                val nextReadBytes = readBytes + emptyLine.length + content.length + line.getBytes().length
                post.get(name) match {
                  case Some(t) => t match {
                    case v: Array[Any] =>
                      val nextV = v ++ Array(value)
                      val nextPost = post ++ Map(name -> nextV)
                      pParseMultiPartFormData(nextPost, nextReadBytes)

                    case v: Any =>
                      val nextPost = post ++ Map(name -> Array(v, value))
                      pParseMultiPartFormData(nextPost, nextReadBytes)
                  }

                  // Mapにkeyが登録されてない時
                  case None =>
                    val nextPost = post ++ Map(name -> value)
                    pParseMultiPartFormData(nextPost, nextReadBytes)
                }

              case _ =>
                println("no match")
                pParseMultiPartFormData(post, readBytes + line.getBytes().length)
            }
          case line =>
            println("non match ")
            pParseMultiPartFormData(post, readBytes + line.getBytes().length)
        }
      }
    }
    pParseMultiPartFormData(Map(), 0)
  }

  def readAll(socket: Socket, contentLength: Int, readLength: Int): Unit = readLine(socket) match {
    case line if(readLength + line.length >= contentLength) =>
      println(scala.io.Source.fromBytes(line).mkString)
    case line =>
      println("---------++++++++++++--------------")
      println(scala.io.Source.fromBytes(line).mkString)
      readAll(socket, contentLength, readLength + line.length)
  }

  // boundaryまで入力を読み込む
  def readLine(socket: Socket, boundary: String): Array[Byte] = {
    val input = socket.getInputStream
    def pReadLine(acc: Array[Byte]): Array[Byte] = input.read match {
      case ch if(scala.io.Source.fromBytes(acc ++ Array(ch.toByte)).mkString.endsWith(boundary)) => acc ++ Array(ch.toByte)
      case ch =>
        pReadLine(acc ++ Array(ch.toByte))
    }
    pReadLine(Array())
  }

  // CRLFまで入力を読み込む
  def readLine(socket: Socket): Array[Byte] = {
    val input = socket.getInputStream
    def pReadLine(acc: Array[Byte]): Array[Byte] = input.read match {
      case '\n' if(acc.last == '\r'.toByte) => acc ++ Array('\n'.toByte)
      case ch => pReadLine(acc ++ Array(ch.toByte))
    }
    pReadLine(Array())
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