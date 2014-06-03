package org.eigengo.activator.nashorn

import akka.actor.ActorRef
import spray.routing.{Directives, Route}
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import spray.http.{ContentTypes, StatusCodes, HttpEntity, HttpResponse}
import org.json4s.{NoTypeHints, Formats}
import org.json4s.jackson.Serialization

trait WorkflowService extends Directives {
  import WorkflowActor._
  import WorkflowInstanceActor._
  import akka.pattern.ask
  import Serialization._

  private implicit val _: Formats = Serialization.formats(NoTypeHints)
  private implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  private def toJson(value: Any): HttpResponse = value match {
    case Started(id, instruction) =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction)))))
    case End(id, instruction, data) =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction), "data" -> WorkflowObjectMapper.map(data)))))
    case Next(id, instruction, data) =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(Map("id" -> id, "instruction" -> WorkflowObjectMapper.map(instruction), "data" -> WorkflowObjectMapper.map(data)))))
    case Error(id, error, instruction, data) =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(Map("id" -> id).toString())), status = StatusCodes.BadRequest)
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
