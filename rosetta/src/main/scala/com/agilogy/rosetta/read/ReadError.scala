package com.agilogy.rosetta.read

import cats.Semigroup
import cats.data.NonEmptyList
import cats.implicits._

import com.github.ghik.silencer.silent

import com.agilogy.rosetta.read.ReadError.{ AtomicReadError, ParseError, ReadErrors, WrongTypeReadError }

sealed trait Segment extends Product with Serializable
object Segment {
  def apply(attribute: String): Segment = Attribute(attribute)
  final case class Attribute(name: String) extends Segment {
    override def toString: String = name
  }
  def apply(position: Int): Segment = ArrayElement(position)
  final case class ArrayElement(position: Int) extends Segment {
    override def toString: String = position.toString
  }
}

sealed trait ReadError extends Exception {
  def at(attribute: String): ReadError = at(Segment.Attribute(attribute))
  def at(path: Segment*): ReadError = NonEmptyList.fromList(path.toList).fold[ReadError](this) { p =>
    this match {
      case a: AtomicReadError     => ReadErrors(NonEmptyList.of(p -> a))
      case ReadErrors(errors)     => ReadErrors(errors.map(_.leftMap(p ::: _)))
      case parseError: ParseError => parseError
    }
  }

  def ++(other: ReadError): ReadError = (this, other) match {
    case (ReadErrors(errs1), ReadErrors(errs2))             => ReadErrors(errs1 ::: errs2)
    case (WrongTypeReadError("Object", msg), ReadErrors(_)) => WrongTypeReadError("Object", msg)
    case (ReadErrors(_), WrongTypeReadError("Object", msg)) => WrongTypeReadError("Object", msg)
    case _                                                  => this
  }

}

object ReadError {

  final case class ParseError(message: String, underlying: Throwable) extends ReadError

  sealed abstract case class ReadErrors(errors: NonEmptyList[(NonEmptyList[Segment], AtomicReadError)])
      extends ReadError {
    override def getMessage: String = s"Error reading record: ${errors.toList.mkString(",")}"

    private def pathToString(path: NonEmptyList[Segment]) = path.toList.mkString("/")
    override def toString: String =
      s"ReadErrors(${errors.map { case (path, error) => s"${pathToString(path)} -> ${error.toString}" }.toList.mkString(",")})"
  }

  object ReadErrors {
    def apply(errors: NonEmptyList[(NonEmptyList[Segment], AtomicReadError)]): ReadErrors =
      new ReadErrors(NonEmptyList.fromListUnsafe(errors.toList.distinct)) {}
  }

  sealed trait AtomicReadError extends ReadError {
    def message: String
    override def getMessage: String = message
  }

  def wrongType(expectedType: String): AtomicReadError = WrongTypeReadError(expectedType)
  final case class WrongTypeReadError(expectedType: String, details: Option[String] = None) extends AtomicReadError {
    override def message: String = s"Wrong type error. Expected $expectedType.${details.map(" " + _).getOrElse("")}"
  }

  @silent("StringPlusAny")
  final case class NativeReadError[E](message: String, error: E) extends AtomicReadError {
    override def getMessage: String = s"$message ($error)"
  }
  case object MissingAttributeError extends AtomicReadError {
    override def message: String = s"Attribute is required but missing"
  }

  implicit val readErrorSemigroup: Semigroup[ReadError] = new Semigroup[ReadError] {
    override def combine(x: ReadError, y: ReadError): ReadError = x ++ y
  }
}
