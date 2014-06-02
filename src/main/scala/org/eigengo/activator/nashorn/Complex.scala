package org.eigengo.activator.nashorn

import java.nio.file.{Paths, Files}
import scala.util.{Failure, Success}
import scala.concurrent.{Future, ExecutionContext}
import javax.script.ScriptEngineManager

object Complex extends App {

  def loadImage(name: String): Array[Byte] = Files.readAllBytes(Paths.get(Complex.getClass.getResource(name).toURI))

  val ocr = new LocalOcrEngine
  val vision = new LocalVisionEngine
  val biometric = new LocalBiometricEngine

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
        |var imports = new JavaImporter(org.eigengo.activator.nashorn.japi);
        |
        |with (imports) {
        |  var text = Future.adapt(ocr.recognise(poster));
        |  var kittenPrint = Future.adapt(biometric.encodeKitten(kitten));
        |  var posterKittenPrint = Future.adapt(vision.extractKitten(poster)).flatMap(executor, function(x) {
        |    if (x.isDefined()) return Future.adapt(biometric.encodeKitten(x.get().kitten()));
        |    else               return Future.failed(new java.lang.RuntimeException("No kitten"));
        |  });
        |
        |  kittenPrint.zip(posterKittenPrint).flatMap(executor, function(x) {
        |     return Future.adapt(biometric.compareKittens(x));
        |  }).zip(text).onComplete2(executor,
        |     function(x) { print("Match!! " + x._1() + ", contact " + x._2()); },
        |     function(x) { print("Failed " + x.getMessage()); }
        |  );
        |}
      """.stripMargin, bindings)
  }

  //scala()
  nashorn()

  Console.readLine()

}
