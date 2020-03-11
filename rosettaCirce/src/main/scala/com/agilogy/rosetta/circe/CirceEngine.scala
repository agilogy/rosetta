package com.agilogy.rosetta.circe

import cats.data.NonEmptyList
import cats.implicits._

import com.github.ghik.silencer.silent
import io.circe.{ parser, CursorOp, Decoder, DecodingFailure, Encoder, Error, Json }

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
    case "C[A]" => ReadError.SimpleMessageReadError("Array expected")
    case _      => ReadError.SimpleMessageReadError(s"$message expected")
  }

  @silent("automatic toString")
  @silent("Recursion")
  def mapReadError(e: Error): ReadError = e match {
    case DecodingFailure("Attempt to decode value on failed cursor", _ :: tail) =>
      mapReadError(DecodingFailure("Object", tail))
    case DecodingFailure(msg, path) =>
      ReadError(
        mapAtomicError(msg),
        path
          .foldLeft(List.empty[Segment]) {
            case (acc, CursorOp.DownField(field))                      => Segment.Attribute(field) :: acc
            case (Segment.ArrayElement(i) :: tail, CursorOp.DownArray) => Segment.ArrayElement(i) :: tail
            case (acc, CursorOp.DownArray)                             => Segment.ArrayElement(0) :: acc
            case (Segment.ArrayElement(i) :: tail, CursorOp.MoveRight) => Segment.ArrayElement(i + 1) :: tail
            case (acc, CursorOp.MoveRight)                             => Segment.ArrayElement(1) :: acc
            case (acc, c) =>
              println(s"Got a $c when I had a $acc")
              Segment.Attribute(c.toString) :: acc
          }
      )
    case error => ReadError.NativeReadError(error.getMessage, error)
  }
}

object CirceStringEngine extends CirceEngine[String] {

  override def readNative[A: NR](input: String): Either[ReadError, A] =
    parser.decodeAccumulating[A](input).leftMap(CirceEngine.mapReadErrors).toEither
}

object CirceJsonEngine extends CirceEngine[Json] {
  override def readNative[A: NR](input: Json): Either[ReadError, A] = input.as[A].leftMap(CirceEngine.mapReadError)

}
