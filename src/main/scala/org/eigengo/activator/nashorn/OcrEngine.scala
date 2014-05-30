package org.eigengo.activator.nashorn

import scala.concurrent.Future
import org.eigengo.activator.nashorn.OcrEngine.{ParsedOcrResult, RawOcrResult, OcrResult}

object OcrEngine {

  sealed trait OcrResult
  case class RawOcrResult(rawText: String) extends OcrResult
  case class ParsedOcrResult(fields: Map[String, String]) extends OcrResult

}

class OcrEngine {

  /**
   * Rather silly implementation of OCR. It fails when it receives empty image, otherwise
   * randomly fails completely, or succeeds with either ``RawOcrResult`` or ``ParsedOcrResult``.
   *
   * @param image the input image
   * @return the future of the result
   */
  def recognise(image: Array[Byte]): Future[OcrResult] = {
    if (image.length == 0) Future.failed(new RuntimeException("Empty input"))

    if (math.random < 0.5) Future.successful(RawOcrResult("Sed ut perspiciatis unde omnis iste natus error sit."))
    else                   Future.successful(ParsedOcrResult(Map("email" -> "sales@typesafe.com")))
  }

}
