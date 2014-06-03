package org.eigengo.activator.nashorn

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptObject
import javax.script.{SimpleScriptContext, ScriptContext, ScriptEngineManager}
import scala.concurrent.ExecutionContext
import java.util

trait ToMapInstructionAndDataMapper {

  val expandPrimitiveArrays = false

  private def map(value: AnyRef): Any = {
    import scala.collection.JavaConversions._

    value match {
      case x: ScriptObjectMirror => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a -> map(x.get(a))))
      case x: ScriptObject => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a.toString -> map(x.get(a))))
      case x: Array[Byte] => if (expandPrimitiveArrays) util.Arrays.toString(x) else "[]"
      case x => x
    }
  }

  def mapInstruction(instruction: AnyRef): Any = map(instruction)
  def mapData(data: AnyRef): Any = map(data)
}

trait PassthroughErrorMapper {
  def mapError(error: AnyRef): AnyRef = error
}

abstract class WorkflowInstance[I, D, E](script: String, variables: Map[String, AnyRef])
                                     (onNext: (I, D) => Unit)
                                     (onEnd: (I, D) => Unit)
                                     (onError: (E, I, D) => Unit) {

  def mapInstruction(instruction: AnyRef): I
  def mapData(data: AnyRef): D
  def mapError(error: AnyRef): E

  private val engine = new ScriptEngineManager().getEngineByName("nashorn")

  //import scala.collection.JavaConversions._
  //private val instance = new WorkflowInstanceBridge(mapAsJavaMap(variables), this)

  // set up bindings
  private var data: AnyRef = _

  // instance state
  private var ended: Boolean = false

  // load the workflow
  private val workflow = engine.eval(script)

  // compute initial state
  var currentState = engine.eval("workflow.states[0];", contextWith("workflow" -> workflow))
  val initialInstruction = engine.eval("workflow.initialInstruction", contextWith("workflow" -> workflow))

  onNext(mapInstruction(initialInstruction), mapData(data))

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

  // flow operation
  def next(stateName: String): Unit = next(stateName, null)
  def next(stateName: String, newData: AnyRef): Unit = next(stateName, newData, null)
  def next(stateName: String, newData: AnyRef, instruction: AnyRef): Unit = {
    data = mergeData(newData)
    currentState = findState(stateName)

    onNext(mapInstruction(instruction), mapData(data))
  }

  def end(newData: AnyRef): Unit = end(newData, null)
  def end(newData: AnyRef, instruction: AnyRef): Unit = {
    data = mergeData(newData)
    ended = true

    onEnd(mapInstruction(instruction), mapData(data))
  }

  def error(error: AnyRef, newData: AnyRef, instruction: AnyRef): Unit = {
    data = mergeData(newData)

    onError(mapError(error), mapInstruction(instruction), mapData(data))
  }

  // request submission
  def tell[A](request: A)(implicit executor: ExecutionContext): Unit = {
    if (!ended) {
      val bindings = engine.createBindings()
      bindings.put("executor", executor)
      variables.foreach { case (k, v) => bindings.put(k, v)}
      engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
      engine.eval("currentState.run(instance, request, data);",
        //contextWith("currentState" -> currentState, "request" -> request, "instance" -> instance, "data" -> data))
        contextWith("currentState" -> currentState, "request" -> request, "instance" -> this, "data" -> data))
    }
  }
}
