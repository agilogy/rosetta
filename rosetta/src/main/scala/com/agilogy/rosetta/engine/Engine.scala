package com.agilogy.rosetta.engine

import com.agilogy.rosetta.read.{ NativeRead, Read, ReadError }
import com.agilogy.rosetta.rw.ReadWrite
import com.agilogy.rosetta.write.{ NativeWrite, ObjectWrite, Write }

trait Engine[NR[_], I, E, NW[_], O] {

  final type R[A] = Read[NR, E, A]
  final def R[A](implicit ev: Read[NR, E, A]): Read[NR, E, A] = ev

  final type W[A]  = Write[NW, A]
  final type OW[A] = ObjectWrite[NW, A]
  final def W[A](implicit ev: W[A]): W[A] = ev

  final type RW[A] = ReadWrite[NR, NW, E, A]
  final def RW[A](implicit readEvidence: Read[NR, E, A], writeEvidence: Write[NW, A]): RW[A] =
    ReadWrite(readEvidence, writeEvidence)

  implicit def nativeReadInstance: NativeRead[NR, E]

  def optionalAttributeNativeRead[A: NR](attributeName: String): NR[Option[A]]
  def attributeNativeRead[A: NR](attributeName: String): NR[A]

  def readNative[A: NR](input: I): Either[ReadError, A]
  def read[A](input: I)(implicit reader: R[A]): Either[ReadError, A] = readNative(input)(reader.nativeReader)

  def listNativeRead[A: NR]: NR[List[A]]

  implicit final def listRead[A](implicit ER: R[A]): R[List[A]] = Read.of(listNativeRead[A](ER.nativeReader))

  implicit def nativeWriteInstance: NativeWrite[NW]

  def writeNative[A: NW](value: A): O
  final def write[A](value: A)(implicit writer: W[A]): O = writeNative(value)(writer.nativeWriter)

  def listNativeWrite[A: NW]: NW[List[A]]
  implicit def listWrite[A: W]: W[List[A]] = Write.of(listNativeWrite[A](W[A].nativeWriter))

  implicit final def listRW[A](implicit R: R[A], W: W[A]): RW[List[A]] = ReadWrite(listRead[A](R), listWrite[A](W))
}
