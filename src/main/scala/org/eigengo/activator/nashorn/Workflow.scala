package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import javax.script.ScriptEngineManager

object Workflow extends App {
  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Complex.getClass.getResource(name).toURI))

  val ocr = new OcrEngine
  val vision = new VisionEngine
  val biometric = new BiometricEngine
  val engine = new ScriptEngineManager().getEngineByName("nashorn")
  val bindings = engine.createBindings()
//  bindings.put("ocr", ocr)
//  bindings.put("vision", vision)
//  bindings.put("biometric", biometric)
//  bindings.put("poster", loadImage("/kittens/lost.jpg"))
//  bindings.put("kitten", loadImage("/kittens/k2.jpg"))
//  bindings.put("next", { message: String => println(message) }.asJavaFunction )
//  bindings.put("executor", ExecutionContext.Implicits.global)

  val x = engine.eval("function() { return function(x) { return x + 5; } }")
  println(x)

}
