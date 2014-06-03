package org.eigengo.activator.nashorn

import spray.routing.{Route, HttpServiceActor}
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

class WorkflowWebapp(route: Route) extends HttpServiceActor {
  def receive: Receive = runRoute(route)
}

object WorkflowWebapp extends App with WorkflowService {
  implicit val system = ActorSystem()
  import system.dispatcher

  val workflowActor = system.actorOf(Props[WorkflowActor])

  val service = system.actorOf(Props(new WorkflowWebapp(
    workflowRoute(workflowActor)
  )))

  IO(Http) ! Http.Bind(service, "0.0.0.0", port = 8080)

  System.in.read()
  system.shutdown()
}