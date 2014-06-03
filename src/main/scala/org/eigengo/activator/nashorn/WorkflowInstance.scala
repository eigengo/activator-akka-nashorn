package org.eigengo.activator.nashorn

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptObject
import javax.script.{SimpleScriptContext, ScriptContext, ScriptEngineManager}
import scala.concurrent.ExecutionContext
import java.util
import org.eigengo.activator.nashorn.WorkflowInstance.{OnError, OnEnd, OnNext, OnInitialized}

object WorkflowInstance {
  type Instruction = Any
  type Data = Any
  type Error = Any

  type OnInitialized = (Instruction) => Unit
  type OnNext = (Instruction, Data) => Unit
  type OnEnd = (Instruction, Data) => Unit
  type OnError = (Error, Instruction, Data) => Unit
}

class WorkflowInstance(script: String, variables: Map[String, AnyRef])(onInitialized: OnInitialized) {

  private val engine = new ScriptEngineManager().getEngineByName("nashorn")

  // set up bindings
  private var data: AnyRef = _

  // instance state
  private var ended: Boolean = false

  // load the workflow
  private val workflow = engine.eval(script)

  // compute initial state
  var currentState = engine.eval("workflow.states[0];", contextWith("workflow" -> workflow))
  val initialInstruction = engine.eval("workflow.initialInstruction", contextWith("workflow" -> workflow))

  onInitialized(initialInstruction)

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

  private class WorkflowInstanceMirror(onNext: OnNext, onEnd: OnEnd, onError: OnError) {
    def next(stateName: String): Unit = next(stateName, null)
    def next(stateName: String, newData: AnyRef): Unit = next(stateName, newData, null)
    def next(stateName: String, newData: AnyRef, instruction: AnyRef): Unit = {
      data = mergeData(newData)
      currentState = findState(stateName)

      onNext(instruction, data)
    }

    def end(newData: AnyRef): Unit = end(newData, null)
    def end(newData: AnyRef, instruction: AnyRef): Unit = {
      data = mergeData(newData)
      ended = true

      onEnd(instruction, data)
    }

    def error(error: AnyRef, newData: AnyRef, instruction: AnyRef): Unit = {
      data = mergeData(newData)

      onError(error, instruction, data)
    }

  }

  // request submission
  def tell[A](request: A, onNext: OnNext, onEnd: OnEnd, onError: OnError)(implicit executor: ExecutionContext): Unit = {
    if (!ended) {
      val bindings = engine.createBindings()
      val mirror = new WorkflowInstanceMirror(onNext, onEnd, onError)
      bindings.put("executor", executor)
      variables.foreach { case (k, v) => bindings.put(k, v)}
      engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
      engine.eval("currentState.run(instance, request, data);",
        contextWith("currentState" -> currentState, "request" -> request, "instance" -> mirror, "data" -> data))
    }
  }
}
