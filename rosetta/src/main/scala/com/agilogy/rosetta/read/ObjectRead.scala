package com.agilogy.rosetta.read

import cats.implicits._
import cats.{ Functor, Semigroupal }

import com.agilogy.rosetta.schema.Schema.{ AttributeSchema, RecordSchema }

trait ObjectRead[NR[_], E, A] { self =>

  private[rosetta] def readAttributes: List[AttributeSchema]
  def nativeReader: NR[A]
  implicit def nativeRead: NativeRead[NR, E]

  def map[B](f: A => B): ObjectRead[NR, E, B] =
    ObjectRead(readAttributes, nativeReader.map(f))

  def leftMap(f: E => E): ObjectRead[NR, E, A] =
    ObjectRead(readAttributes, nativeRead.leftMap(nativeReader)(f))

  def andThen[B](f: A => Either[E, B]): ObjectRead[NR, E, B] =
    ObjectRead(readAttributes, nativeRead.andThen(nativeReader)(f))

  def product[B](fb: ObjectRead[NR, E, B]): ObjectRead[NR, E, (A, B)] = {
    val newAttributes = self.readAttributes ::: fb.readAttributes
    ObjectRead.apply(newAttributes, self.nativeReader.product(fb.nativeReader))
  }

  def apply(name: String): Read[NR, E, A] = Read.of(nativeReader, RecordSchema(name, readAttributes))
}

object ObjectRead {

  def apply[NR[_], E, A](attrs: List[AttributeSchema], read: NR[A])(
    implicit N: NativeRead[NR, E]
  ): ObjectRead[NR, E, A] =
    new ObjectRead[NR, E, A] {
      override private[rosetta] def readAttributes: List[AttributeSchema] = attrs
      override implicit def nativeRead: NativeRead[NR, E]                 = N
      override def nativeReader: NR[A]                                    = read
    }

  implicit def objectReaderFunctor[NR[_], E]: Functor[ObjectRead[NR, E, *]] =
    new Functor[ObjectRead[NR, E, *]] {
      override def map[A, B](fa: ObjectRead[NR, E, A])(f: A => B): ObjectRead[NR, E, B] = fa.map(f)
    }
  implicit def objectReaderSemigroupal[NR[_], E]: Semigroupal[ObjectRead[NR, E, *]] =
    new Semigroupal[ObjectRead[NR, E, *]] {
      override def product[A, B](fa: ObjectRead[NR, E, A], fb: ObjectRead[NR, E, B]): ObjectRead[NR, E, (A, B)] =
        fa.product(fb)
    }
}
