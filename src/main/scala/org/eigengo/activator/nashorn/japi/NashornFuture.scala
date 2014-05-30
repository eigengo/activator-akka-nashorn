package org.eigengo.activator.nashorn.japi

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.util.function

object NashornFuture {

  def failed(t: Throwable): NashornFuture[_] = new NashornFuture(Future.failed(t))

  def fromScala[A](future: Future[A]): NashornFuture[A] = new NashornFuture(future)

}

class NashornFuture[A](private val future: Future[A]) {

  def flatMap[U](f: java.util.function.Function[A, NashornFuture[U]], executor: ExecutionContext): NashornFuture[U] =
    new NashornFuture(future.flatMap(x => f(x).future)(executor))

  def zip[U](that: NashornFuture[U]): NashornFuture[(A, U)] = new NashornFuture(this.future.zip(that.future))

  def onComplete(f: java.util.function.Function[Try[A], _], executor: ExecutionContext): Unit =
    future.onComplete(x => f(x))(executor)

  def onComplete2(s: java.util.function.Function[A, _], f: java.util.function.Function[Throwable, _], executor: ExecutionContext): Unit = {
    future.onComplete {
      case Success(x) => s(x)
      case Failure(t) => f(t)
    }(executor)
  }

}


object NashornFunction {
  
  implicit class JavaFunctionConversion[A, U](f: A => U) {

    def asJavaFunction: java.util.function.Function[A, U] = new NashornFunction(f)

  }

  class NashornFunction[A, U](f: A => U) extends java.util.function.Function[A, U] {

    override def apply(t: A): U = f(t)

    override def compose[V](before: function.Function[_ >: V, _ <: A]): function.Function[V, U] =
      new NashornFunction({ t: V => f(before.apply(t))})

    override def andThen[V](after: function.Function[_ >: U, _ <: V]): function.Function[A, V] =
      new NashornFunction({ t: A => after.apply(f(t))})
  }
  
}

