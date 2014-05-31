package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, ExecutionContext}
import javax.script.ScriptEngineManager

object Complex extends App {

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
    import org.eigengo.activator.nashorn.japi.Function._
    val engine = new ScriptEngineManager().getEngineByName("nashorn")
    val bindings = engine.createBindings()
    bindings.put("ocr", ocr)
    bindings.put("vision", vision)
    bindings.put("biometric", biometric)
    bindings.put("poster", loadImage("/kittens/lost.jpg"))
    bindings.put("kitten", loadImage("/kittens/k2.jpg"))
    bindings.put("next", { message: String => println(message) }.asJavaFunction )
    bindings.put("executor", ExecutionContext.Implicits.global)

    engine.eval(
      """
        |var imports = new JavaImporter(org.eigengo.activator.nashorn.japi);
        |
        |with (imports) {
        |  var text = Future.fromScala(ocr.recognise(poster));
        |  var kittenPrint = Future.fromScala(biometric.encodeKitten(kitten));
        |  var posterKittenPrint = Future.fromScala(vision.extractKitten(poster)).flatMap(function(x) {
        |    if (x.isDefined()) return Future.fromScala(biometric.encodeKitten(x.get().kitten()));
        |    else               return Future.failed(new java.lang.RuntimeException("No kitten"));
        |  }, executor);
        |
        |  kittenPrint.zip(posterKittenPrint).flatMap(function(x) {
        |     return Future.fromScala(biometric.compareKittens(x));
        |  }, executor).zip(text).onComplete2(
        |     function(x) { next(new java.lang.String("Match!! " + x._1() + ", contact " + x._2())); },
        |     function(x) { next(new java.lang.String("Failed " + x.getMessage())); },
        |     executor);
        |}
      """.stripMargin, bindings)
  }

  //scala()
  nashorn()

  Console.readLine()

}
