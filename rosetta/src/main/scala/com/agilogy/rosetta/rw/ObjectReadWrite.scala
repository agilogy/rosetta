package com.agilogy.rosetta.rw

import cats.{ Invariant, Semigroupal }

import com.agilogy.rosetta.read.{ NativeRead, ObjectRead, Read }
import com.agilogy.rosetta.write.{ NativeWrite, ObjectWrite, Write }

final case class ObjectReadWrite[NR[_], NW[_], E, A](reader: ObjectRead[NR, E, A], writer: ObjectWrite[NW, A])
    extends ObjectWrite[NW, A]
    with Read[NR, E, A] {

  override implicit val nativeRead: NativeRead[NR, E] = reader.nativeRead

  override def nativeReader: NR[A] = reader.nativeReader

  override private[rosetta] def attributes: List[(String, Write[NW, A])] = writer.attributes
  override implicit def nativeWrite: NativeWrite[NW]                     = writer.nativeWrite

  def imap[B](f: A => B)(g: B => A): ObjectReadWrite[NR, NW, E, B] = ObjectReadWrite(reader.map(f), writer.contramap(g))
  def iAndThen[B](f: A => Either[E, B])(g: B => A): ObjectReadWrite[NR, NW, E, B] =
    ObjectReadWrite(reader.andThen(f), writer.contramap(g))

  def product[O, B](fb: ObjectReadWrite[NR, NW, E, B]): ObjectReadWrite[NR, NW, E, (A, B)] =
    ObjectReadWrite(reader.product(fb.reader), writer.product(fb.writer))

  override def apply(name: String): ReadWrite[NR, NW, E, A] = ReadWrite.of(reader(name), writer(name))
}

object ObjectReadWrite {
  implicit def objectRWInvariantInstance[NR[_], NW[_], E]: Invariant[ObjectReadWrite[NR, NW, E, *]] =
    new Invariant[ObjectReadWrite[NR, NW, E, *]] {
      override def imap[A, B](fa: ObjectReadWrite[NR, NW, E, A])(f: A => B)(g: B => A): ObjectReadWrite[NR, NW, E, B] =
        fa.imap(f)(g)
    }

  implicit def objectRWSemigroupal[NR[_]: Semigroupal, NW[_]: NativeWrite, E]
    : Semigroupal[ObjectReadWrite[NR, NW, E, *]] =
    new Semigroupal[ObjectReadWrite[NR, NW, E, *]] {
      override def product[A, B](
        fa: ObjectReadWrite[NR, NW, E, A],
        fb: ObjectReadWrite[NR, NW, E, B]
      ): ObjectReadWrite[NR, NW, E, (A, B)] =
        fa.product(fb)
    }
}
