package org.eigengo.activator.nashorn

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptObject
import javax.script.{Bindings, SimpleScriptContext, ScriptContext, ScriptEngineManager}
import scala.concurrent.ExecutionContext

trait MapInstructionAndData {
  type Instruction = Any
  type Data = Any

  private def map(value: AnyRef): Any = {
    import scala.collection.JavaConversions._

    value match {
      case x: ScriptObjectMirror => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a -> map(x.get(a))))
      case x: ScriptObject => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a.toString -> map(x.get(a))))
      case x => x
    }
  }

  def mapInstruction(instruction: AnyRef): Instruction = map(instruction)
  def mapData(data: AnyRef): Data = map(data)
}

trait WorkflowInstanceOperations {

  def next(stateName: String): Unit = next(stateName, null)
  def next(stateName: String, newData: AnyRef): Unit = next(stateName, newData, null)
  def next(stateName: String, newData: AnyRef, instruction: AnyRef): Unit

  def end(newData: AnyRef): Unit = end(newData, null)
  def end(newData: AnyRef, instruction: AnyRef): Unit
}

abstract class WorkflowInstance[I, D](script: String, variables: Map[String, AnyRef])(onResponse: (I, D) => Unit)
  extends WorkflowInstanceOperations {

  def mapInstruction(instruction: AnyRef): I
  def mapData(data: AnyRef): D

  private val engine = new ScriptEngineManager().getEngineByName("nashorn")

  //import scala.collection.JavaConversions._
  //private val instance = new WorkflowInstanceBridge(mapAsJavaMap(variables), this)

  // set up bindings
  private var data: AnyRef = _

  // load the workflow
  private val workflow = engine.eval(script)

  // compute initial state
  var currentState = engine.eval("workflow.states[0];", contextWith("workflow" -> workflow))
  val initialInstruction = engine.eval("workflow.initialInstruction", contextWith("workflow" -> workflow))

  respond(data, initialInstruction)

  private def contextWith(elements: (String, Any)*): ScriptContext = {
    val ctx = new SimpleScriptContext()
    elements.foreach { case (k, v) => ctx.setAttribute(k, v, ScriptContext.ENGINE_SCOPE) }
    ctx
  }

  private def mergeData(newData: AnyRef): AnyRef = {
    val d = engine.eval(
      """
        |if (data == null) data = newData;
        |if (newData != null) {
        |    for (var property in newData) {
        |        if (newData.hasOwnProperty(property)) data[property] = newData[property];
        |    }
        |}
        |data
      """.stripMargin, contextWith("data" -> data, "newData" -> newData))
    if (d == null) throw new RuntimeException("Data is null.")
    d
  }

  private def findState(stateName: String) = {

    val state = engine.eval(
      """
        |var state = null;
        |for (var i = 0; i < workflow.states.length; i++) {
        |    if (workflow.states[i].name == stateName) {
        |       state = workflow.states[i];
        |       break;
        |    }
        |}
        |state;
      """.stripMargin, contextWith("workflow" -> workflow, "stateName" -> stateName))
    if (state == null) throw new RuntimeException("Cannot find state " + stateName)
    state
  }

  private def respond(data: AnyRef, instruction: AnyRef): Unit = {
    onResponse(mapInstruction(instruction), mapData(data))
  }

  // flow operation
  def next(stateName: String, newData: AnyRef, instruction: AnyRef): Unit = {
    data = mergeData(newData)
    currentState = findState(stateName)

    respond(data, instruction)
  }

  def end(newData: AnyRef, instruction: AnyRef): Unit = {
    data = mergeData(newData)
    respond(data, instruction)
  }

  // request submission
  def tell[A](request: A)(implicit executor: ExecutionContext): Unit = {
    val bindings = engine.createBindings()
    bindings.put("executor", executor)
    variables.foreach { case (k, v) => bindings.put(k, v) }
    engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
    engine.eval("currentState.run(instance, request, data);",
      //contextWith("currentState" -> currentState, "request" -> request, "instance" -> instance, "data" -> data))
      contextWith("currentState" -> currentState, "request" -> request, "instance" -> this, "data" -> data))
  }
}
