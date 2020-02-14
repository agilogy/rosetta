package com.agilogy.rosetta.read

sealed trait Segment extends Product with Serializable
object Segment {
  final case class Attribute(name: String) extends Segment {
    override def toString: String = name
  }
  final case class ArrayElement(position: Int) extends Segment {
    override def toString: String = position.toString
  }
}

sealed trait ReadErrorCause
object ReadErrorCause {
  final case class NativeReadError[E](error: E)        extends ReadErrorCause
  final case class MissingAttribute(attribute: String) extends ReadErrorCause
}
final case class ReadError(cause: ReadErrorCause, path: List[Segment]) extends Exception {
  def at(p: List[Segment]): ReadError = ReadError(cause, p ::: path)

  override def toString: String = s"""ReadError("${path.mkString("/")}", $cause)"""
}
