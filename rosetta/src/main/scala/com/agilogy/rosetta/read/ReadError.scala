package com.agilogy.rosetta.read

import cats.Semigroup
import cats.implicits._

sealed trait Segment extends Product with Serializable
object Segment {
  final case class Attribute(name: String) extends Segment {
    override def toString: String = name
  }
  final case class ArrayElement(position: Int) extends Segment {
    override def toString: String = position.toString
  }
}

sealed trait ReadError extends Exception

object ReadError {

  def ofRecord(errors: (String, ReadError)*): ReadError =
    RecordReadError(errors.map(_.leftMap(Segment.Attribute)).toMap)
  final case class RecordReadError(errors: Map[Segment.Attribute, ReadError]) extends ReadError {
    override def getMessage: String = s"Error reading record: ${errors.mkString(",")}"
  }
  def ofArray(errors: (Int, ReadError)*): ReadError =
    ArrayReadError(errors.map(_.leftMap(Segment.ArrayElement)).toMap)
  final case class ArrayReadError(errors: Map[Segment.ArrayElement, ReadError]) extends ReadError {
    override def getMessage: String = s"Error reading array: ${errors.mkString(",")}"
  }
  sealed trait AtomicReadError extends ReadError {
    def message: String
    override def getMessage: String = message
  }
  def atomic(message: String): ReadError = SimpleMessageReadError(message)
  final case class SimpleMessageReadError(message: String) extends AtomicReadError
  final case class NativeReadError[E](message: String, error: E) extends AtomicReadError {
    override def getMessage: String = s"$message ($error)"
  }
  final case class MissingAttributeError(attribute: Segment.Attribute) extends AtomicReadError {
    override def message: String = s"Attribute ${attribute.name} is required but missing"
  }

  def apply(message: String, segments: List[Segment] = List.empty): ReadError =
    apply(SimpleMessageReadError(message), segments)

  def apply(error: ReadError, segments: List[Segment]): ReadError = segments.reverse.foldLeft(error) {
    case (acc, segment) => apply(acc, segment)
  }

  def apply(error: ReadError, segment: Segment): ReadError = segment match {
    case attributeSegment @ Segment.Attribute(_) => RecordReadError(Map(attributeSegment -> error))
    case arraySegment @ Segment.ArrayElement(_)  => ArrayReadError(Map(arraySegment      -> error))
  }

  implicit val readErrorSemigroup: Semigroup[ReadError] = new Semigroup[ReadError] {
    override def combine(x: ReadError, y: ReadError): ReadError = (x, y) match {
      case (RecordReadError(errs1), RecordReadError(errs2)) => RecordReadError(catsSyntaxSemigroup(errs1) |+| errs2)
      case (ArrayReadError(errs1), ArrayReadError(errs2))   => ArrayReadError(catsSyntaxSemigroup(errs1) |+| errs2)
      case _                                                => x
    }
  }
}
