package org.eigengo.activator.nashorn

import scala.concurrent.Future
import org.eigengo.activator.nashorn.LocalBiometricEngine.KittenPrint

object LocalBiometricEngine {

  case class KittenPrint(magic: Double)

}

class LocalBiometricEngine {

  /**
   * Encodes an image of a kitten to a representation that can be stored & compared later with
   * another kitten to get a match.
   *
   * @param image the image to encode
   * @return the kitten's biometric print
   */
  def encodeKitten(image: Array[Byte]): Future[KittenPrint] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))
    Future.successful(KittenPrint(math.random))
  }

  /**
   * Compares two kittens' biometric prints to compute their similarity. Returns the match rate,
   * where 1.0 means "k1 and k2 are the same kitten", and 0.0 means "k1 and k2 are completely different kittens".
   *
   * @param kittens the kittens to compare
   * @return the match rate
   */
  def compareKittens(kittens: (KittenPrint, KittenPrint)): Future[Double] = Future.successful(math.random)
}
