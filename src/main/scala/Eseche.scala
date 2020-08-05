import java.io.{FileInputStream, InputStream, OutputStream}
import java.net.{ServerSocket, Socket}
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.{ActorSystem, Props}
import ServerActorMessage.Run
import akka.routing.RoundRobinPool

object Eseche extends App {
  private val DOCUMENT_ROOT = "./public"

  val server = new ServerSocket(9999)
  val system = ActorSystem("system")
  val serverActor = system.actorOf(RoundRobinPool(5).props(Props[ServerActor]), "serverActorPool")

  while(true){
    val socket = server.accept()
    serverActor ! Run(socket)
  }
}