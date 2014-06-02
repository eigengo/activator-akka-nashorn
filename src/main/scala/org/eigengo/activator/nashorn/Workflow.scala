package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import scala.io.Source

object Workflow extends App {
  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Workflow.getClass.getResource(name).toURI))
  def loadScript(name: String): String = Source.fromInputStream(Workflow.getClass.getResourceAsStream(name)).mkString

  val localOcr = new LocalOcrEngine
  val localVision = new LocalVisionEngine
  val localBiometric = new LocalBiometricEngine

  val nativeOcr = new NativeOcrEngine
  val nativeVision = new NativeVisionEngine
  val nativeBiometric = new NativeBiometricEngine

  import scala.concurrent.ExecutionContext.Implicits.global

  def onResponse(instruction: Any, data: Any, end: Boolean): Unit = {
    if (end)
      println(s">>> Finished using $instruction with $data")
    else
      println(s">>> Transition using $instruction with $data")
  }

  val structureExample = new WorkflowInstance(loadScript("/structure.js"), Map())(onResponse) with MapInstructionAndData
  structureExample.tell(loadImage("/kittens/lost.jpg"))
  structureExample.tell(loadImage("/kittens/k2.jpg"))
  structureExample.tell(loadImage("/kittens/k1.jpg"))

  println("-------------------------------")
  
  val nativeExample = new WorkflowInstance(
    loadScript("/native.js"),
    Map("ocr" -> nativeOcr, "biometric" -> nativeBiometric, "vision" -> nativeVision))(onResponse) with MapInstructionAndData
  nativeExample.tell(loadImage("/kittens/lost.jpg"))
  Thread.sleep(1000)
  nativeExample.tell(loadImage("/kittens/k2.jpg"))

  println("*******************************")

  Console.readLine()
}
