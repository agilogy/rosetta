package com.agilogy.rosetta.read

import cats.implicits._
import cats.Functor

trait Read[NR[_], E, A] { self =>
  def nativeReader: NR[A]
  implicit def nativeRead: NativeRead[NR, E]

  final def map[B](f: A => B): Read[NR, E, B] = Read.of[NR, E, B](nativeReader.map(f))

  def leftMap(f: E => E): Read[NR, E, A] =
    Read.of[NR, E, A](nativeRead.leftMap(nativeReader)(f))

  def andThen[B](f: A => Either[E, B]): Read[NR, E, B] =
    Read.of[NR, E, B](nativeRead.andThen(nativeReader)(f))
}

object Read {
  def apply[NR[_], E, A](implicit ev: Read[NR, E, A]): Read[NR, E, A] = ev
  def of[NR[_], E, A](reader: NR[A])(implicit ev: NativeRead[NR, E]): Read[NR, E, A] = new Read[NR, E, A] {
    override def nativeReader: NR[A]                    = reader
    override implicit val nativeRead: NativeRead[NR, E] = ev
  }
  implicit def readerFunctor[NR[_], I, E]: Functor[Read[NR, E, *]] = new Functor[Read[NR, E, *]] {
    override def map[A, B](fa: Read[NR, E, A])(f: A => B): Read[NR, E, B] = fa.map(f)
  }
}
