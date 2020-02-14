package com.agilogy.rosetta.read

import cats.implicits._
import cats.{ Functor, Semigroupal }

trait NativeRead[NR[_], E] extends Functor[NR] with Semigroupal[NR] {
  def andThen[A, B](nativeRead: NR[A])(f: A => Either[E, B]): NR[B]
  def leftMap[A](nativeRead: NR[A])(f: E => E): NR[A]

  override final def map[A, B](fa: NR[A])(f: A => B): NR[B] = andThen(fa)(f(_).asRight)
}

object NativeRead {
  type NRBi[NR[_], x, y] = NR[y]
  def apply[NR[_], E](implicit ev: NativeRead[NR, E]): NativeRead[NR, E] = ev
}
