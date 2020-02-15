package com.agilogy.rosetta.write

import cats.{ Contravariant, Semigroupal }

import com.agilogy.rosetta.schema.Schema.RecordSchema

trait ObjectWrite[NW[_], A] { self =>

  private[rosetta] def attributes: List[(String, Write[NW, A])]
  implicit def nativeWrite: NativeWrite[NW]

  def contramap[B](f: B => A): ObjectWrite[NW, B] =
    ObjectWrite(attributes.map {
      case (name, writes) => (name, writes.contramap(f))
    })

  def product[B](fb: ObjectWrite[NW, B]): ObjectWrite[NW, (A, B)] = {
    val newAttributes = self.contramap[(A, B)](_._1).attributes ++ fb.contramap[(A, B)](_._2).attributes
    ObjectWrite.apply(newAttributes)
  }

  def apply(name: String): Write[NW, A] =
    Write.of(
      nativeWrite.nativeObjectWriter(name, attributes.map {
        case (name, writes) => (name, writes.nativeWriter)
      }),
      RecordSchema(name, attributes.map {
        case (name, w) => (name, w.schema)
      })
    )
}

object ObjectWrite {

  def apply[NW[_], A](attrs: List[(String, Write[NW, A])])(implicit N: NativeWrite[NW]): ObjectWrite[NW, A] =
    new ObjectWrite[NW, A] {
      override private[rosetta] def attributes: List[(String, Write[NW, A])] = attrs
      override implicit def nativeWrite: NativeWrite[NW]                     = N
    }

  implicit def objectWriterContravariant[NW[_]]: Contravariant[ObjectWrite[NW, *]] =
    new Contravariant[ObjectWrite[NW, *]] {
      override def contramap[A, B](fa: ObjectWrite[NW, A])(f: B => A): ObjectWrite[NW, B] = fa.contramap(f)
    }
  implicit def objectWriterSemigroupal[NW[_]]: Semigroupal[ObjectWrite[NW, *]] =
    new Semigroupal[ObjectWrite[NW, *]] {
      override def product[A, B](fa: ObjectWrite[NW, A], fb: ObjectWrite[NW, B]): ObjectWrite[NW, (A, B)] =
        fa.product(fb)
    }
}
