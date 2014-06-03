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

  def onNext(instruction: Any, data: Any): Unit = {
    println(s">>> Transition using $instruction with $data")
  }

  def onEnd(instruction: Any, data: Any): Unit = {
    println(s">>> Finished using $instruction with $data")
  }

  def onError(error: Any, instruction: Any, data: Any): Unit = {
    println(s">>> Error $error using $instruction with $data")
  }

  val structureExample = new WorkflowInstance(loadScript("/structure.js"), Map())(onNext)(onEnd)(onError) with ToMapInstructionAndDataMapper with PassthroughErrorMapper
  structureExample.tell(loadImage("/kittens/lost.jpg"))
  structureExample.tell(loadImage("/kittens/k2.jpg"))
  structureExample.tell(loadImage("/kittens/k1.jpg"))

  println("-------------------------------")
  
  val nativeExample = new WorkflowInstance(
    loadScript("/native.js"),
    Map("ocr" -> nativeOcr, "biometric" -> nativeBiometric, "vision" -> nativeVision))(onNext)(onEnd)(onError) with ToMapInstructionAndDataMapper with PassthroughErrorMapper
  nativeExample.tell(loadImage("/kittens/lost.jpg"))
  Thread.sleep(1000)
  nativeExample.tell(loadImage("/kittens/k2.jpg"))

  println("*******************************")

  Console.readLine()
}
