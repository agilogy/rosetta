package com.agilogy.rosetta.rw

import cats.Contravariant

import com.agilogy.rosetta.read.{ NativeRead, Read }
import com.agilogy.rosetta.schema.Schema
import com.agilogy.rosetta.write.Write

trait ReadWrite[NR[_], NW[_], E, A] extends Read[NR, E, A] with Write[NW, A] {
  def imap[B](f: A => B)(g: B => A): ReadWrite[NR, NW, E, B] =
    ReadWrite.of(this.map(f), this.contramap(g))
  def iAndThen[B](f: A => Either[E, B])(g: B => A): ReadWrite[NR, NW, E, B] =
    ReadWrite.of(this.andThen(f), this.contramap(g))
}

object ReadWrite {
  def of[NR[_], NW[_], E, A](reader: Read[NR, E, A], writer: Write[NW, A]): ReadWrite[NR, NW, E, A] =
    new ReadWrite[NR, NW, E, A] {
      override def nativeWriter: NW[A]                       = writer.nativeWriter
      override def nativeReader: NR[A]                       = reader.nativeReader
      override implicit val nativeRead: NativeRead[NR, E]    = reader.nativeRead
      override implicit def contravariant: Contravariant[NW] = writer.contravariant
      override def schema: Schema                            = writer.schema
    }
}
