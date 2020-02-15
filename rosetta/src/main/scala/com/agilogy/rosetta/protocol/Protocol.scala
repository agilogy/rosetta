package com.agilogy.rosetta.protocol

import scala.reflect.ClassTag

import cats.implicits._

import com.agilogy.rosetta.engine.Engine
import com.agilogy.rosetta.read.{ NativeRead, ObjectRead, Read }
import com.agilogy.rosetta.rw.{ ObjectReadWrite, ReadWrite }
import com.agilogy.rosetta.schema.Schema.AtomSchema
import com.agilogy.rosetta.write.{ NativeWrite, ObjectWrite, Write }

abstract class Protocol[NR[_], I, E, NW[_], O](engine: Engine[NR, I, E, NW, O]) {

  def R[A](implicit ev: Read[NR, E, A]): Read[NR, E, A]                                                  = ev
  def W[A](implicit ev: Write[NW, A]): Write[NW, A]                                                      = ev
  def RW[A](implicit readEvidence: Read[NR, E, A], writeEvidence: Write[NW, A]): ReadWrite[NR, NW, E, A] = engine.RW[A]

  implicit final def reader[A](implicit nativeReader: NR[A]): Read[NR, E, A] = Read.of(nativeReader)

  protected implicit def nativeReadInstance: NativeRead[NR, E] = engine.nativeReadInstance

  protected implicit class ReaderStringSyntax(self: String) {
    def read[A: Read[NR, E, *]]: ObjectRead[NR, E, A] =
      ObjectRead(List(self), engine.attributeNativeRead(self)(Read[NR, E, A].nativeReader))
    def readOr[A: Read[NR, E, *]](orElse: A): ObjectRead[NR, E, A] = readOpt[A].map(_.getOrElse(orElse))
    def readOpt[A: Read[NR, E, *]]: ObjectRead[NR, E, Option[A]] =
      ObjectRead(List(self), engine.optionalAttributeNativeRead(self)(Read[NR, E, A].nativeReader))

  }

  protected implicit def nativeWriteInstance: NativeWrite[NW] = engine.nativeWriteInstance

  protected implicit final class WriterStringSyntax(self: String) {
    def write[A: Write[NW, *]]: ObjectWrite[NW, A] = ObjectWrite(List(self -> Write[NW, A]))
  }

  protected final implicit def writer[A: ClassTag](implicit nativeWriter: NW[A]): Write[NW, A] = {
    val classTagToString = implicitly[ClassTag[A]].toString()
    val lastDot          = classTagToString.lastIndexOf(".")
    Write.of(
      nativeWriter,
      AtomSchema(if (lastDot === -1) classTagToString else classTagToString.substring(lastDot + 1))
    )
  }

  protected implicit class RWStringSyntax(self: String) {
    def rw[A: Read[NR, E, *]: Write[NW, *]]: ObjectReadWrite[NR, NW, E, A] =
      ObjectReadWrite(self.read[A], self.write[A])
  }

}
