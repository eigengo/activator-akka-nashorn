package org.eigengo.activator.nashorn.japi

import scala.concurrent.{ExecutionContext}
import scala.util.{Failure, Success, Try}
import java.util.function

object Future {

  def failed(t: Throwable): Future[_] = new Future(scala.concurrent.Future.failed(t))

  def fromScala[A](future: scala.concurrent.Future[A]): Future[A] = new Future(future)

}

class Future[A](private val future: scala.concurrent.Future[A]) {

  def flatMap[U](f: java.util.function.Function[A, Future[U]], executor: ExecutionContext): Future[U] =
    new Future(future.flatMap(x => f(x).future)(executor))

  def zip[U](that: Future[U]): Future[(A, U)] = new Future(this.future.zip(that.future))

  def onComplete(f: java.util.function.Function[Try[A], _], executor: ExecutionContext): Unit =
    future.onComplete(x => f(x))(executor)

  def onComplete2(s: java.util.function.Function[A, _], f: java.util.function.Function[Throwable, _], executor: ExecutionContext): Unit = {
    future.onComplete {
      case Success(x) => s(x)
      case Failure(t) => f(t)
    }(executor)
  }

}


object Function {
  
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

