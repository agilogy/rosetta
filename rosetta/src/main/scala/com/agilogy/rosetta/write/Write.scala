package com.agilogy.rosetta.write

import cats.Contravariant
import cats.implicits._

trait Write[NW[_], A] {
  implicit def contravariant: Contravariant[NW]
  def nativeWriter: NW[A]
  def contramap[B](f: B => A): Write[NW, B] = Write.of(nativeWriter.contramap(f))
}

object Write {
  def apply[NW[_], A](implicit ev: Write[NW, A]): Write[NW, A] = ev
  def of[NW[_], A](writer: NW[A])(implicit C: Contravariant[NW]): Write[NW, A] = new Write[NW, A] {
    override def nativeWriter: NW[A]                       = writer
    override implicit def contravariant: Contravariant[NW] = C
  }
  implicit def contravariantWriter[NW[_]: Contravariant, O]: Contravariant[Write[NW, *]] =
    new Contravariant[Write[NW, *]] {
      override def contramap[A, B](fa: Write[NW, A])(f: B => A): Write[NW, B] =
        Write.of[NW, B](fa.nativeWriter.contramap(f))
    }
}