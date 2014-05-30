package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, ExecutionContext}
import javax.script.ScriptEngineManager

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

  def nashorn(): Unit = {
    val engine = new ScriptEngineManager().getEngineByName("nashorn")
    val bindings = engine.createBindings()
    bindings.put("ocr", ocr)
    bindings.put("vision", vision)
    bindings.put("biometric", biometric)
    bindings.put("poster", loadImage("/kittens/lost.jpg"))
    bindings.put("kitten", loadImage("/kittens/k2.jpg"))
    bindings.put("executor", ExecutionContext.Implicits.global)

    engine.eval(
      """
        |var imports = new JavaImporter(org.eigengo.activator.nashorn);
        |
        |with (imports) {
        |  var text = ocr.recogniseJ(poster);
        |  var kittenPrint = biometric.encodeKittenJ(kitten);
        |  var posterKittenPrint = vision.extractKittenJ(poster).flatMap(function(x) {
        |    if (x.isDefined()) return biometric.encodeKittenJ(x.get().kitten());
        |    else               return NashornFuture.failed(new RuntimeException("No kitten"))
        |  }, executor);
        |
        |  kittenPrint.zip(posterKittenPrint).flatMap(function(x) {
        |     return biometric.compareKittensJ(x);
        |  }, executor).zip(text).onComplete(function(x) { print(x); }, executor);
        |}
      """.stripMargin, bindings)
  }

  //scala()
  nashorn()

  Console.readLine()

}
