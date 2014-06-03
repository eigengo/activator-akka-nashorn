package org.eigengo.activator.nashorn

import scala.concurrent.Future

class NativeVisionEngine {

  /**
   * Extracts the line in the form of a kitten (viz https://www.youtube.com/watch?v=BKorP55Aqvg) from
   * the given image. It doesn't actually do it, it merely simulates the effort. I leave the actual
   * implementation to the curious readers.
   *
   * @param image the input image
   * @return maybe the extracted kitten
   */
  def extractKitten(image: Array[Byte]): Future[Array[Byte]] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))

    val json =
      if (math.random < 0.9) """{"coordinates":{"x":0, "y":0, "w":100, "h":100}, "kitten":[4, 5, 13, 5, 234, 12, 40]}"""
      else                   """{}"""

    Future.successful(json.getBytes)
  }

}
