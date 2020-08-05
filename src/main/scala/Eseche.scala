import java.net.{ServerSocket}
import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinPool

import ServerActorMessage.Run

object Eseche extends App {
  val server = new ServerSocket(9999)
  val system = ActorSystem("system")
  val serverActor = system.actorOf(RoundRobinPool(5).props(Props[ServerActor]), "serverActorPool")

  while(true){
    val socket = server.accept()
    serverActor ! Run(socket)
  }
}