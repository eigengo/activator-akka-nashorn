package org.eigengo.activator.nashorn

import akka.actor.Actor

class WorkflowActor(workflowName: String) extends Actor {
  val nativeOcr = new NativeOcrEngine
  val nativeVision = new NativeVisionEngine
  val nativeBiometric = new NativeBiometricEngine

  def receive: Receive = {
    case image: Array[Byte] =>
      // we have an input frame
  }

}
