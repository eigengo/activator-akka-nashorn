package org.eigengo.activator.nashorn

import akka.actor.{Props, Actor}
import scala.util.{Failure, Success, Try}
import scala.io.Source
import java.util.UUID

object WorkflowActor {
  case class StartWorkflow(workflowName: String) extends AnyVal
  case class Request[A](id: String, request: A)
  case class Started(id: String, instruction: Any)

  val nativeOcr = new NativeOcrEngine
  val nativeVision = new NativeVisionEngine
  val nativeBiometric = new NativeBiometricEngine
}

class WorkflowActor extends Actor{
  import WorkflowActor._

  private def loadScript(name: String): Try[String] = Try(Source.fromInputStream(Workflow.getClass.getResourceAsStream(name)).mkString)

  def receive: Receive = {
    case StartWorkflow(workflowName) =>
      loadScript(workflowName) match {
        case Success(script) =>
          val id = UUID.randomUUID().toString
          val instance = new WorkflowInstance(script, Map("ocr" -> nativeOcr, "biometric" -> nativeBiometric, "vision" -> nativeVision))(i =>
            sender() ! Started(id, i)
          )
          context.actorOf(Props(new WorkflowInstanceActor(instance)), name = id)
        case Failure(_) =>
          // :(
      }
    case Request(id, request) =>
      context.actorSelection(id).tell(request, sender)
  }

}
