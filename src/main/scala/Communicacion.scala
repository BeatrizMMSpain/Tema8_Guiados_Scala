import Comprobador.{ComprobarUsuario, UsuarioBueno, UsuarioIntruso}
import Recorder.NuevoUsuario
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.language.postfixOps
import Almacenamiento.AñadirUsuario
case class Usuario(username: String, email: String)

object Recorder{
  sealed trait RecorderMsg
  case class NuevoUsuario(user: Usuario) extends RecorderMsg
}

object Almacenamiento{
  sealed trait AlmacenamientoMsg
  case class AñadirUsuario(user: Usuario) extends AlmacenamientoMsg
}

object Comprobador{
  sealed trait ComprobarMsg
  // Mensajes Checker
  case class ComprobarUsuario(user: Usuario) extends ComprobarMsg
  sealed trait ComprobarRespuesta
  // Respuestas Checker
  case class UsuarioIntruso(user: Usuario) extends ComprobarMsg
  case class UsuarioBueno(user: Usuario) extends ComprobarMsg
}

class Recorder(comprobador: ActorRef, almacenamiento: ActorRef) extends Actor{
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout=Timeout(5 seconds)
  /*
  Actor Recorder tiene la referencia de los actores "Comprobador" y
 "Almacenamiento" y enviaremos el usuario al "Comprobador" para
 comprobar si es un usuario que esta en lista negra o no
  */
  def receive = {
    case NuevoUsuario(usuario) =>
      comprobador ? ComprobarUsuario(usuario) map {
        case UsuarioBueno(usuario)=>
          almacenamiento ! Almacenamiento.AñadirUsuario(usuario)
        case UsuarioIntruso(usuario)=>
          println(s"Recorder: $usuario en la lista negra")
      }
  }
}

class Almacenamiento extends Actor{
  var usuarios =List.empty[Usuario]
  /*
  El actor "Almacemiento" recibe un mensaje de "Recorder" con el
 usuario que insertaremos en nuestra lista de usuarios en la lista
 blanca.
  */
  def receive= {
    case Almacenamiento.AñadirUsuario(user) =>
      println(s"Almacenamiento: $user añadido")
      usuarios = user :: usuarios
  }
}

class Comprobador extends Actor {
  val blackList = List(
    Usuario("Sergio", "sergio@imaginagroup.com")
  )

  def receive = {
    case ComprobarUsuario(usuario) if blackList.contains(usuario) =>
      println(s"Comprobar: $usuario en la lista negra.")
      sender() ! UsuarioIntruso(usuario)
    case ComprobarUsuario(usuario) =>
      println(s"Comprobador: $usuario no está en la lista negra.")
      sender() ! UsuarioBueno(usuario)
  }
}

object Comunicacion extends App{
  val system = ActorSystem("comunicacion-con-actores")
  val comprobador = system.actorOf(Props[Comprobador],"comprobador")
  val almacenamiento = system.actorOf(Props[Almacenamiento],"almacenamiento")
  val recorder = system.actorOf(Props(new Recorder(comprobador,almacenamiento)),"recorder")

  // Enviamos un nuevo mensaje para añadir un usuario nuevo
  recorder ! Recorder.NuevoUsuario(Usuario("Alejandro","alejandro@imaginagroup.com"))
  recorder ! Recorder.NuevoUsuario(Usuario("Alfredo","alfredo@imaginagroup.com"))
  recorder ! Recorder.NuevoUsuario(Usuario("Sergio","sergio@imaginagroup.com"))
  Thread.sleep(100)
  // Cerramos el sistema
  println("Hasta Luego!")
  system.terminate()
}