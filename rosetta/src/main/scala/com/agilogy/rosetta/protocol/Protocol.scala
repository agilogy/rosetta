package com.agilogy.rosetta.protocol

import scala.reflect.ClassTag

import cats.implicits._

import com.agilogy.rosetta.engine.Engine
import com.agilogy.rosetta.read.{ NativeRead, ObjectRead, Read }
import com.agilogy.rosetta.rw.{ ObjectReadWrite, ReadWrite }
import com.agilogy.rosetta.schema.AttributeRequirement.{ Mandatory, Optional }
import com.agilogy.rosetta.schema.Schema.AttributeSchema
import com.agilogy.rosetta.schema.{ AttributeRequirement, Schema }
import com.agilogy.rosetta.write.{ NativeWrite, ObjectWrite, Write }

abstract class Protocol[NR[_], I, E, NW[_], O](engine: Engine[NR, I, E, NW, O]) {

  def R[A](implicit ev: Read[NR, E, A]): Read[NR, E, A]                                                  = ev
  def W[A](implicit ev: Write[NW, A]): Write[NW, A]                                                      = ev
  def RW[A](implicit readEvidence: Read[NR, E, A], writeEvidence: Write[NW, A]): ReadWrite[NR, NW, E, A] = engine.RW[A]

  implicit final def reader[A: ClassTag](implicit nativeReader: NR[A]): Read[NR, E, A] =
    Read.of(nativeReader, Schema.fromClass[A])

  protected implicit def nativeReadInstance: NativeRead[NR, E] = engine.nativeReadInstance

  protected implicit class ReaderStringSyntax(self: String) {
    def read[A: Read[NR, E, *]]: ObjectRead[NR, E, A] =
      ObjectRead(
        List(AttributeSchema(self, Read[NR, E, A].schema, AttributeRequirement.Mandatory)),
        engine.attributeNativeRead(self)(Read[NR, E, A].nativeReader)
      )
    def readOr[A: Read[NR, E, *]](orElse: A): ObjectRead[NR, E, A] =
      ObjectRead(
        List(AttributeSchema(self, Read[NR, E, A].schema, AttributeRequirement.DefaultsTo(orElse))),
        engine.optionalAttributeNativeRead(self)(Read[NR, E, A].nativeReader).map(_.getOrElse(orElse))
      )
    def readOpt[A: Read[NR, E, *]]: ObjectRead[NR, E, Option[A]] =
      ObjectRead(
        List(AttributeSchema(self, Read[NR, E, A].schema, AttributeRequirement.Optional)),
        engine.optionalAttributeNativeRead(self)(Read[NR, E, A].nativeReader)
      )

  }

  protected implicit def nativeWriteInstance: NativeWrite[NW] = engine.nativeWriteInstance

  protected implicit final class WriterStringSyntax(self: String) {
    private def optionWrite[A: Write[NW, *]]: Write[NW, Option[A]] =
      Write.of(engine.optionalNativeWrite[A](W[A].nativeWriter), W[A].schema)
    def write[A: Write[NW, *]]: ObjectWrite[NW, A] =
      ObjectWrite(List(AttributeSchema(self, Write[NW, A].schema, Mandatory) -> Write[NW, A]))
    def writeOpt[A: Write[NW, *]]: ObjectWrite[NW, Option[A]] =
      ObjectWrite(List(AttributeSchema(self, Write[NW, A].schema, Optional) -> optionWrite[A]))
  }

  protected final implicit def writer[A: ClassTag](implicit nativeWriter: NW[A]): Write[NW, A] =
    Write.of(nativeWriter, Schema.fromClass[A])

  protected implicit class RWStringSyntax(self: String) {
    def rw[A: Read[NR, E, *]: Write[NW, *]]: ObjectReadWrite[NR, NW, E, A] =
      ObjectReadWrite(self.read[A], self.write[A])
  }

}
