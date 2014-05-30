package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, ExecutionContext}

object Complex extends App {

  val noImage: Array[Byte] = Array.ofDim(0)
  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Complex.getClass.getResource(name).toURI))

  val ocr = new OcrEngine
  val vision = new VisionEngine
  val biometric = new BiometricEngine

  def scala(): Unit = {
    import ExecutionContext.Implicits.global

    val poster = loadImage("/kittens/lost.jpg")
    val kitten = loadImage("/kittens/k2.jpg")
    
    val text = ocr.recognise(poster)
    val kittenPrint = biometric.encodeKitten(kitten)
    val posterKittenPrint = vision.extractKitten(poster).flatMap {
      case None =>               Future.failed(new RuntimeException("No kitten"))
      case Some(posterKitten) => biometric.encodeKitten(posterKitten.kitten)
    }

    kittenPrint.zip(posterKittenPrint).flatMap(biometric.compareKittens).zip(text).onComplete {
      case Success((matchRate, text)) => println(s"Match!!! $matchRate. Contact $text")
      case Failure(x)                 => println(s"Failed ${x.getMessage}")
    }
  }

  scala()

  Console.readLine()

}
