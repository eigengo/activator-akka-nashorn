package org.eigengo.activator.nashorn

import akka.actor.ActorRef
import spray.routing.{Directives, Route}
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import spray.http.{StatusCodes, HttpEntity, HttpResponse}

trait WorkflowService extends Directives {
  import WorkflowActor._
  import WorkflowInstanceActor._
  import akka.pattern.ask

  private implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  private def toJson(value: Any): HttpResponse = value match {
    case Started(id, instruction) =>
      HttpResponse(entity = HttpEntity(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction)).toString()))
    case End(id, instruction, data) =>
      HttpResponse(entity = HttpEntity(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction), "data" -> WorkflowObjectMapper.map(data)).toString()))
    case Next(id, instruction, data) =>
      HttpResponse(entity = HttpEntity(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction), "data" -> WorkflowObjectMapper.map(data)).toString()))
    case Error(id, error, instruction, data) =>
      HttpResponse(entity = HttpEntity(Map("id" -> id).toString()), status = StatusCodes.BadRequest)
  }


  def workflowRoute(workflowActor: ActorRef)(implicit executor: ExecutionContext): Route = {
    path("workflow" / "start" / Segment) { workflowName =>
      post {
        complete {
          (workflowActor ? StartWorkflow("/" + workflowName)).map(toJson)
        }
      }
    } ~
    path("workflow" / "next" / Segment) { id =>
      post {
        handleWith { entity: Array[Byte] =>
          println(entity)
          (workflowActor ? Request(id, entity)).map(toJson)
        }
      }
    }
  }

}
