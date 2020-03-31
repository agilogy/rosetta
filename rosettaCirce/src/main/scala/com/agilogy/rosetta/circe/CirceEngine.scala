package com.agilogy.rosetta.circe

import cats.data.NonEmptyList

import com.github.ghik.silencer.silent
import io.circe.parser.decodeAccumulating
import io.circe.{ CursorOp, Decoder, DecodingFailure, Encoder, Error, Json, ParsingFailure }

import com.agilogy.rosetta.read.ReadError.AtomicReadError
import com.agilogy.rosetta.read.{ ReadError, Segment }

object CirceEngine {

  def decode[A: Decoder](s: String): Either[ReadError, A] =
    decodeAccumulating[A](s).leftMap(CirceEngine.mapReadErrors).toEither

  def decode[A: Decoder](json: Json): Either[ReadError, A] =
    Decoder[A].decodeAccumulating(json.hcursor).leftMap(CirceEngine.mapReadErrors).toEither

  def encode[A: Encoder](a: A): Json = Encoder[A].apply(a)

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
      ReadError.wrongType("Array").at(cursorOpsToSegments(historyTail): _*)
    case DecodingFailure("Attempt to decode value on failed cursor", path @ CursorOp.DownField(_) :: _) =>
      ReadError.MissingAttributeError.at(cursorOpsToSegments(path): _*)
    case DecodingFailure("[A]Option[A]", CursorOp.DownField(_) :: parent) =>
      ReadError.wrongType("Object").at(cursorOpsToSegments(parent): _*)
    case DecodingFailure(msg, path) =>
      mapAtomicError(msg).at(cursorOpsToSegments(path): _*)
    case ParsingFailure(message, underlying) =>
      ReadError.ParseError(message, underlying)
  }
}
