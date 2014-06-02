package org.eigengo.activator.nashorn

import scala.concurrent.Future

class NativeBiometricEngine {
  /**
   * Encodes an image of a kitten to a representation that can be stored & compared later with
   * another kitten to get a match.
   *
   * @param image the image to encode
   * @return the kitten's biometric print
   */
  def encodeKitten(image: Array[Byte]): Future[Array[Byte]] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))

    Future.successful(Array.fill(1024)((System.currentTimeMillis() % 255).toByte))
  }

  /**
   * Compares two kittens' biometric prints to compute their similarity. Returns the match rate,
   * where 1.0 means "k1 and k2 are the same kitten", and 0.0 means "k1 and k2 are completely different kittens".
   *
   * @param k1 the kitten to compare
   * @param k2 the kitten to compare
   * @return the match rate
   */
  def compareKittens(k1: Array[Byte], k2: Array[Byte]): Future[Array[Byte]] = Future.successful("""{"result":0.5}""".getBytes)
}
