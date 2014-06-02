package org.eigengo.activator.nashorn.japi

import scala.concurrent.{ExecutionContext}
import scala.util.{Failure, Success, Try}
import java.util.function
import java.util.function.BiFunction

object Future {

  def failed(t: Throwable): Future[_] = new Future(scala.concurrent.Future.failed(t))

  def adapt[A](future: scala.concurrent.Future[A]): Future[A] = new Future(future)

}

class Future[A](private val future: scala.concurrent.Future[A]) {

  def flatMap[U](executor: ExecutionContext, f: java.util.function.Function[A, Future[U]]): Future[U] =
    new Future(future.flatMap(x => f(x).future)(executor))

  def zip[U](that: Future[U]): Future[(A, U)] = new Future(this.future.zip(that.future))

  def onComplete(executor: ExecutionContext, f: java.util.function.Function[Try[A], _]): Unit =
    future.onComplete(x => f(x))(executor)

  def onComplete2(executor: ExecutionContext, s: java.util.function.Function[A, _], f: java.util.function.Function[Throwable, _]): Unit = {
    future.onComplete {
      case Success(x) => s(x)
      case Failure(t) => f(t)
    }(executor)
  }

}

/*
object Function {
  
  implicit class JavaFunctionConversion[A, U](f: A => U) {
    def asJavaFunction: java.util.function.Function[A, U] = new NashornFunction1(f)
  }

  implicit class JavaFunctionConversion2[A, B, U](f: (A, B) => U) {
    def asJavaFunction: java.util.function.BiFunction[A, B, U] = new NashornFunction2(f)
  }

  class NashornFunction2[A, B, U](f: (A, B) => U) extends java.util.function.BiFunction[A, B, U] {
    override def apply(t: A, u: B): U = f(t, u)

    override def andThen[V](after: function.Function[_ >: U, _ <: V]): BiFunction[A, B, V] =
      new NashornFunction2( { (t: A, u: B) => after.apply(f(t, u)) })
  }

  class NashornFunction1[A, U](f: A => U) extends java.util.function.Function[A, U] {

    override def apply(t: A): U = f(t)

    override def compose[V](before: function.Function[_ >: V, _ <: A]): function.Function[V, U] =
      new NashornFunction1({ t: V => f(before.apply(t))})

    override def andThen[V](after: function.Function[_ >: U, _ <: V]): function.Function[A, V] =
      new NashornFunction1({ t: A => after.apply(f(t))})
  }
  
}
*/