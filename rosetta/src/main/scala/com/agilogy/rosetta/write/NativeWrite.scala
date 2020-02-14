package com.agilogy.rosetta.write

import cats.Contravariant

trait NativeWrite[NW[_]] extends Contravariant[NW] {
  def nativeObjectWriter[A](name: String, attributes: List[(String, NW[A])]): NW[A]
}

object NativeWrite {
  def apply[NW[_]](implicit ev: NativeWrite[NW]): NativeWrite[NW] = ev
}
