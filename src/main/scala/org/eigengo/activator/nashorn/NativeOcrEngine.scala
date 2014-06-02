package org.eigengo.activator.nashorn

import scala.concurrent.Future

class NativeOcrEngine {

  /**
   * Rather silly implementation of OCR. It fails when it receives empty image, otherwise
   * randomly fails completely, or succeeds with either ``RawOcrResult`` or ``ParsedOcrResult``.
   *
   * @param image the input image
   * @return the future of the result
   */
  def recognise(image: Array[Byte]): Future[Array[Byte]] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))

    val json =
      if (math.random < 0.5) """{ "body":"Sed ut perspiciatis unde omnis iste natus error sit."}"""
      else                   """{ "body":"Sed ut perspiciatis unde omnis iste natus error sit.", "fields":{"email":"sales@typesafe.com"}}"""

    Future.successful(json.getBytes)
  }

}
