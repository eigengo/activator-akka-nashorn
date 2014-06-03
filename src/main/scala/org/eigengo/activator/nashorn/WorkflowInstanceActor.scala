package org.eigengo.activator.nashorn

import akka.actor.{ActorRef, Actor}

object WorkflowInstanceActor {

  case class Next(id: String, instruction: Any, data: Any)
  case class End(id: String, instruction: Any, data: Any)
  case class Error(id: String, error: Any, instruction: Any, data: Any)
}

class WorkflowInstanceActor(instance: WorkflowInstance) extends Actor {
  import WorkflowInstanceActor._
  import context.dispatcher

  def receive: Receive = {
    case image: Array[Byte] =>
      instance.tell(image, onNext(sender()), onEnd(sender()), onError(sender()))
  }

  private def onNext(sender: ActorRef)(instruction: Any, data: Any): Unit = {
    sender ! Next(self.path.name, instruction, data)
  }

  private def onError(sender: ActorRef)(error: Any, instruction: Any, data: Any): Unit = {
    sender ! Error(self.path.name, error, instruction, data)
  }

  private def onEnd(sender: ActorRef)(instruction: Any, data: Any): Unit = {
    sender ! End(self.path.name, instruction, data)
    context.stop(self)
  }

}
