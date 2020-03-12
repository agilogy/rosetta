package com.agilogy.rosetta.circe

import cats.data.NonEmptyList
import cats.implicits._

import com.github.ghik.silencer.silent
import io.circe.{ parser, CursorOp, Decoder, DecodingFailure, Encoder, Error, Json, ParsingFailure }
import com.agilogy.rosetta.engine.Engine
import com.agilogy.rosetta.read.ReadError.AtomicReadError
import com.agilogy.rosetta.read.{ NativeRead, ReadError, Segment }
import com.agilogy.rosetta.write.NativeWrite

trait CirceEngine[I] extends Engine[Decoder, I, DecodingFailure, Encoder, String] {

  protected final type NR[A] = Decoder[A]

  override final def optionalAttributeNativeRead[A: NR](attributeName: String): NR[Option[A]] =
    Decoder.decodeOption(implicitly[NR[A]]).at(attributeName)

  override def attributeNativeRead[A: NR](attributeName: String): NR[A] = Decoder[A].at(attributeName)

  override final def listNativeRead[A: NR]: NR[List[A]] = Decoder.decodeList

  override implicit final def nativeReadInstance: NativeRead[Decoder, DecodingFailure] =
    new NativeRead[Decoder, DecodingFailure] {

      override def product[A, B](fa: Decoder[A], fb: Decoder[B]): Decoder[(A, B)] = fa.product(fb)

      override def andThen[A, B](nativeRead: NR[A])(f: A => Either[DecodingFailure, B]): NR[B] =
        nativeRead.emapTry(a => f(a).toTry)

      override def leftMap[A](nativeRead: NR[A])(f: DecodingFailure => DecodingFailure): NR[A] =
        nativeRead.adaptErr { case x => f(x) }
    }
  type NW[A] = Encoder[A]

  override def writeNative[A: Encoder](value: A): String = Encoder[A].apply(value).noSpaces
  override def listNativeWrite[A: NW]: NW[List[A]]       = Encoder.encodeList[A]
  override def optionalNativeWrite[A: NW]: NW[Option[A]] = Encoder[Option[A]]

  override implicit def nativeWriteInstance: NativeWrite[Encoder] = new NativeWrite[Encoder] {

    override def nativeObjectWriter[A](name: String, attributes: List[(String, NW[A])]): Encoder[A] = {

      @silent("Recursion")
      def f(attributes: List[(String, NW[A])]): Encoder[A] =
        attributes match {
          case Nil             => Encoder.instance(_ => Json.obj())
          case (a1, e1) :: Nil => Encoder.forProduct1(a1)(identity[A])(e1)
          case l =>
            val (l0, l1) = l.splitAt(l.length / 2)
            Encoder(a => f(l0)(a) deepMerge f(l1)(a))
        }

      f(attributes.reverse)
    }

    override def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = fa.contramap(f)
  }

}

object CirceEngine {

  def mapReadErrors(errors: NonEmptyList[Error]): ReadError = errors.map(mapReadError).reduce

  private def mapAtomicError(message: String): AtomicReadError = message match {
    case "C[A]" => ReadError.WrongTypeReadError("Array")
    case _      => ReadError.WrongTypeReadError(message)
  }

  // Used the implementation of opsToPath from Circe as an inspiration
  @silent("ToString")
  private def cursorOpsToSegments(history: List[CursorOp]): List[Segment] =
    history
      .foldRight(List.empty[Segment]) {
        case (CursorOp.DownField(k), acc)                          => Segment.Attribute(k) :: acc
        case (CursorOp.DownArray, acc)                             => Segment.ArrayElement(0) :: acc
        case (CursorOp.MoveUp, _ :: tail)                          => tail
        case (CursorOp.MoveRight, Segment.ArrayElement(i) :: tail) => Segment.ArrayElement(i + 1) :: tail
        case (CursorOp.MoveLeft, Segment.ArrayElement(i) :: tail)  => Segment.ArrayElement(i - 1) :: tail
        case (CursorOp.RightN(n), Segment.ArrayElement(i) :: tail) => Segment.ArrayElement(i + n) :: tail
        case (CursorOp.LeftN(n), Segment.ArrayElement(i) :: tail)  => Segment.ArrayElement(i - n) :: tail
        case (op, acc) =>
          println(s"Got a $op when I had a $acc")
          Segment.Attribute(op.toString) :: acc
      }
      .reverse

  def mapReadError(e: Error): ReadError = e match {
    case DecodingFailure("Attempt to decode value on failed cursor", CursorOp.DownArray :: historyTail) =>
      ReadError(ReadError.wrongType("Array"), cursorOpsToSegments(historyTail))
    case DecodingFailure("Attempt to decode value on failed cursor", path @ CursorOp.DownField(_) :: _) =>
      ReadError(ReadError.MissingAttributeError, cursorOpsToSegments(path))
    case DecodingFailure("[A]Option[A]", CursorOp.DownField(_) :: parent) =>
      ReadError(ReadError.wrongType("Object"), cursorOpsToSegments(parent))
    case DecodingFailure(msg, path) =>
      ReadError(mapAtomicError(msg), cursorOpsToSegments(path))
    case ParsingFailure(message, underlying) => ReadError.ParseError(message, underlying)
  }
}

object CirceStringEngine extends CirceEngine[String] {

  override def readNative[A: NR](input: String): Either[ReadError, A] =
    parser.decodeAccumulating[A](input).leftMap(CirceEngine.mapReadErrors).toEither
}

object CirceJsonEngine extends CirceEngine[Json] {
  override def readNative[A: NR](input: Json): Either[ReadError, A] = input.as[A].leftMap(CirceEngine.mapReadError)

}
