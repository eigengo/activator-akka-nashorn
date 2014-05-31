package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import javax.script.ScriptEngineManager
import scala.io.Source

object Workflow extends App {
  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Workflow.getClass.getResource(name).toURI))
  def loadScript(name: String): String = Source.fromInputStream(Workflow.getClass.getResourceAsStream(name)).mkString
  val ocr = new OcrEngine
  val vision = new VisionEngine
  val biometric = new BiometricEngine

  class WorkflowInstance(name: String) {
    import japi.Function._
    private val engine = new ScriptEngineManager().getEngineByName("nashorn")
    private val bindings = engine.createBindings()

    // set up bindings
    bindings.put("ocr", ocr)
    bindings.put("vision", vision)
    bindings.put("biometric", biometric)
    bindings.put("next", (next _).asJavaFunction)
    bindings.put("data", null)

    // load the workflow
    private val workflow = engine.eval(loadScript(name))

    // compute initial state
    bindings.put("workflow", workflow)
    private val initialState = engine.eval("workflow.states[0];", bindings)
    val initialTransition = engine.eval("workflow.initialTransition", bindings)
    bindings.put("currentState", initialState)
    bindings.put("instance", this)

    private def mergeData(newData: AnyRef): Unit = {
      bindings.put("newData", newData)
      engine.eval(
        """
          |if (data == null) data = newData;
          |else for (var property in newData) {
          |    if (newData.hasOwnProperty(property)) {
          |        data[property] = newData[property];
          |    }
          |}
        """.stripMargin, bindings)
      bindings.remove("newData")
    }

    private def findState(stateName: String) = {
      bindings.put("stateName", stateName)
      val state = engine.eval(
        """
          |var state = null;
          |for (i = 0; i < workflow.states.length; i++)
          |    if (workflow.states[i].name == stateName) {
          |       state = workflow.states[i];
          |       break;
          |    }
          |state;
        """.stripMargin, bindings)
      bindings.remove("stateName")
      state
    }

    // flow operation
    def next(stateName: String, data: AnyRef): Unit = {
      mergeData(data)
      bindings.put("currentState", findState(stateName))
      println("*** moving on to " + stateName + " using " + data)
    }

    def end(data: AnyRef): Unit = {
      println("*** ended with " + data)
    }

    // request submission
    def input[A](request: A): Unit = {
      bindings.put("request", request)
      engine.eval("currentState.run(request, instance, data);", bindings)
    }
  }

  val instance = new WorkflowInstance("/structure.js")
  instance.input(loadImage("/kittens/lost.jpg"))
  instance.input(loadImage("/kittens/k2.jpg"))
}
