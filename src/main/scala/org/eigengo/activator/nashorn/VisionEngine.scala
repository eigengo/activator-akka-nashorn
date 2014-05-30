package org.eigengo.activator.nashorn

import scala.concurrent.Future
import org.eigengo.activator.nashorn.VisionEngine.{Rectangle, Kitten}

object VisionEngine {

  case class Rectangle(x: Int, y: Int, w: Int, h: Int)
  case class Kitten(coordinates: Rectangle, kitten: Array[Byte])

}

class VisionEngine {

  /**
   * Extracts the line in the form of a kitten (viz https://www.youtube.com/watch?v=BKorP55Aqvg) from
   * the given image. It doesn't actually do it, it merely simulates the effort. I leave the actual
   * implementation to the curious readers.
   *
   * @param image the input image
   * @return maybe the extracted kitten
   */
  def extractKitten(image: Array[Byte]): Future[Option[Kitten]] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))

    if (math.random < 0.5) Future.successful(Some(Kitten(Rectangle(0, 0, 100, 100), Array.ofDim(1024))))
    else                   Future.successful(None)
  }

}
