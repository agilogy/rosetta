package com.agilogy.rosetta.write

import cats.{ Contravariant, Semigroupal }

trait ObjectWrite[NW[_], A] { self =>

  private[rosetta] def attributes: List[(String, NW[A])]
  implicit def nativeWrite: NativeWrite[NW]

  def contramap[B](f: B => A): ObjectWrite[NW, B] =
    ObjectWrite(attributes.map {
      case (name, writes) => (name, nativeWrite.contramap(writes)(f))
    })

  def product[O, B](fb: ObjectWrite[NW, B]): ObjectWrite[NW, (A, B)] = {
    val newAttributes = self.contramap[(A, B)](_._1).attributes ++ fb.contramap[(A, B)](_._2).attributes
    ObjectWrite.apply(newAttributes)
  }

  def apply(name: String): Write[NW, A] = Write.of(nativeWrite.nativeObjectWriter(name, attributes))
}

object ObjectWrite {

  def apply[NW[_], A](attrs: List[(String, NW[A])])(implicit N: NativeWrite[NW]): ObjectWrite[NW, A] =
    new ObjectWrite[NW, A] {
      override private[rosetta] def attributes: List[(String, NW[A])] = attrs
      override implicit def nativeWrite: NativeWrite[NW]              = N
    }

  implicit def objectWriterContravariant[NW[_]]: Contravariant[ObjectWrite[NW, *]] =
    new Contravariant[ObjectWrite[NW, *]] {
      override def contramap[A, B](fa: ObjectWrite[NW, A])(f: B => A): ObjectWrite[NW, B] = fa.contramap(f)
    }
  implicit def objectWriterSemigroupal[NW[_], O]: Semigroupal[ObjectWrite[NW, *]] =
    new Semigroupal[ObjectWrite[NW, *]] {
      override def product[A, B](fa: ObjectWrite[NW, A], fb: ObjectWrite[NW, B]): ObjectWrite[NW, (A, B)] =
        fa.product(fb)
    }
}
