package com.agilogy.rosetta.read

import cats.implicits._

import com.agilogy.rosetta.schema.Schema

trait Read[NR[_], E, A] { self =>
  def nativeReader: NR[A]
  implicit def nativeRead: NativeRead[NR, E]
  def schema: Schema

  final def map[B](f: A => B): Read[NR, E, B] = Read.of[NR, E, B](nativeReader.map(f), schema)

  def leftMap(f: E => E): Read[NR, E, A] =
    Read.of[NR, E, A](nativeRead.leftMap(nativeReader)(f), schema)

  def andThen[B](f: A => Either[E, B]): Read[NR, E, B] =
    Read.of[NR, E, B](nativeRead.andThen(nativeReader)(f), schema)
}

object Read {
  def apply[NR[_], E, A](implicit ev: Read[NR, E, A]): Read[NR, E, A] = ev
  def of[NR[_], E, A](reader: NR[A], s: => Schema)(implicit ev: NativeRead[NR, E]): Read[NR, E, A] =
    new Read[NR, E, A] {
      override def nativeReader: NR[A]                    = reader
      override implicit val nativeRead: NativeRead[NR, E] = ev
      override lazy val schema: Schema                    = s
    }
//  implicit def readerFunctor[NR[_], I, E]: Functor[Read[NR, E, *]] = new Functor[Read[NR, E, *]] {
//    override def map[A, B](fa: Read[NR, E, A])(f: A => B): Read[NR, E, B] = fa.map(f)
//  }
}
