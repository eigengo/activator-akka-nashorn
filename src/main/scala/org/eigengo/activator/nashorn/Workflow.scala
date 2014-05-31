package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import javax.script.{ScriptContext, SimpleScriptContext, ScriptEngineManager}
import scala.io.Source

object Workflow extends App {
  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Workflow.getClass.getResource(name).toURI))
  def loadScript(name: String): String = Source.fromInputStream(Workflow.getClass.getResourceAsStream(name)).mkString
  val ocr = new OcrEngine
  val vision = new VisionEngine
  val biometric = new BiometricEngine

  class WorkflowInstance(name: String)
                        (onResponse: String => Unit) {
    private val engine = new ScriptEngineManager().getEngineByName("nashorn")
    private val bindings = engine.createBindings()

    // set up bindings
    private var data: AnyRef = _
    bindings.put("ocr", ocr)
    bindings.put("vision", vision)
    bindings.put("biometric", biometric)

    // load the workflow
    private val workflow = engine.eval(loadScript(name))

    // compute initial state
    var currentState = engine.eval("workflow.states[0];", contextWith("workflow" -> workflow))
    val initialTransition = engine.eval("workflow.initialTransition", contextWith("workflow" -> workflow))

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

      bindings.put("stateName", stateName)
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

    private def respond(data: AnyRef, transition: AnyRef): Unit = {
      val x = engine.eval("JSON.stringify(foo.four)",
        contextWith("foo" -> data, "transition" -> transition))
      println(x)
      onResponse("")
    }

    // flow operation
    def next(stateName: String): Unit = next(stateName, null)
    def next(stateName: String, newData: AnyRef): Unit = next(stateName, newData, null)
    def next(stateName: String, newData: AnyRef, transition: AnyRef): Unit = {
      data = mergeData(newData)
      currentState = findState(stateName)

      respond(data, transition)
    }

    def end(newData: AnyRef): Unit = {
      data = mergeData(newData)

      respond(data, null)
    }

    // request submission
    def input[A](request: A): Unit = {
      engine.eval("currentState.run(request, instance, data);",
        contextWith("currentState" -> currentState, "request" -> request, "instance" -> this, "data" -> data))
    }
  }

  def onResponse(data: String): Unit = {
    println(s">>> Reply with $data")
  }

  val instance = new WorkflowInstance("/structure.js")(onResponse)
  instance.input(loadImage("/kittens/lost.jpg"))
  instance.input(loadImage("/kittens/k2.jpg"))
  instance.input(loadImage("/kittens/k1.jpg"))
}
