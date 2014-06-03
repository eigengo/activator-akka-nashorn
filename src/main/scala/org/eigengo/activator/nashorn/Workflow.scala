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

  def onInitialized(instruction: Any): Unit = {
    println(s">>> Started using ${WorkflowObjectMapper.map(instruction)}")
  }

  def onNext(instruction: Any, data: Any): Unit = {
    println(s">>> Transition using ${WorkflowObjectMapper.map(instruction)} with ${WorkflowObjectMapper.map(data)}")
  }

  def onEnd(instruction: Any, data: Any): Unit = {
    println(s">>> Finished using ${WorkflowObjectMapper.map(instruction)} with ${WorkflowObjectMapper.map(data)}")
  }

  def onError(error: Any, instruction: Any, data: Any): Unit = {
    println(s">>> Error ${WorkflowObjectMapper.map(error)} using ${WorkflowObjectMapper.map(instruction)} with ${WorkflowObjectMapper.map(data)}")
  }

  val structureExample = new WorkflowInstance(loadScript("/structure.js"), Map())(onInitialized)
  structureExample.tell(loadImage("/kittens/lost.jpg"), onNext, onEnd, onError)
  structureExample.tell(loadImage("/kittens/k2.jpg"), onNext, onEnd, onError)
  structureExample.tell(loadImage("/kittens/k1.jpg"), onNext, onEnd, onError)

  println("-------------------------------")
  
  val nativeExample = new WorkflowInstance(
    loadScript("/native.js"),
    Map("ocr" -> nativeOcr, "biometric" -> nativeBiometric, "vision" -> nativeVision))(onInitialized)
  nativeExample.tell(loadImage("/kittens/lost.jpg"), onNext, onEnd, onError)
  Thread.sleep(1000)
  nativeExample.tell(loadImage("/kittens/k2.jpg"), onNext, onEnd, onError)

  println("*******************************")

  Console.readLine()
}
