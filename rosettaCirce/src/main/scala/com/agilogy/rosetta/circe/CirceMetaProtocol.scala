package com.agilogy.rosetta.circe

import scala.collection.mutable

import cats.implicits._

import com.agilogy.rosetta.meta.Meta
import com.github.ghik.silencer.silent
import io.circe.{ BuilderDecoder, Decoder, DecodingFailure, Encoder }

object CirceMetaProtocol extends TemplatedCirceEncoders with TemplatedCirceDecoders {

  @silent("AsInstanceOf")
  @silent("unchecked")
  implicit def metaCirceEncoder[A](
    implicit meta: Meta[A],
    unionCodecConfiguration: UnionCodecConfiguration[A] = UnionCodecConfiguration.structureDiscriminator[A]
  ): Encoder[A] =
    (meta match {
      case a: Meta.Atom[A]    => atomMetaCirceEncoder(a)
      case o: Meta.Option[A]  => optionMetaCirceEncoder(o)
      case l: Meta.List[_, A] => listMetaCirceEncoder(l)
      case r: Meta.Record[A]  => recordMetaCirceEncoder(r)
      case u: Meta.Union[A]   => unionMetaCirceEncoder(u, unionCodecConfiguration)
    }).asInstanceOf[Encoder[A]]

  @silent("AsInstanceOf")
  @silent("unchecked")
  implicit def metaCirceDecoder[A](
    implicit meta: Meta[A],
    unionCodecConfiguration: UnionCodecConfiguration[A] = UnionCodecConfiguration.structureDiscriminator[A]
  ): Decoder[A] =
    (meta match {
      case a: Meta.Atom[A]    => atomMetaCirceDecoder(a)
      case o: Meta.Option[A]  => optionMetaCirceDecoder(o)
      case l: Meta.List[_, A] => listMetaCirceDecoder(l)
      case r: Meta.Record[A]  => recordMetaCirceDecoder(r)
      case u: Meta.Union[A]   => unionMetaCirceDecoder(u, unionCodecConfiguration)
    }).asInstanceOf[Decoder[A]]

  @silent("AsInstanceOf")
  def simpleAtomMetaCirceEncoder[A](implicit meta: Meta.SimpleAtom[A]): Encoder[A] =
    (meta match {
      case Meta.boolean => Encoder.encodeBoolean
      case Meta.string  => Encoder.encodeString
      case Meta.char    => Encoder.encodeChar
      case Meta.int     => Encoder.encodeInt
      case Meta.long    => Encoder.encodeLong
      case Meta.integer => Encoder.encodeBigInt
      case Meta.float   => Encoder.encodeFloat
      case Meta.double  => Encoder.encodeDouble
      case Meta.decimal => Encoder.encodeBigDecimal
    }).asInstanceOf[Encoder[A]]

  @silent("AsInstanceOf")
  def simpleAtomMetaCirceDecoder[A](implicit meta: Meta.SimpleAtom[A]): Decoder[A] =
    (meta match {
      case Meta.boolean => Decoder.decodeBoolean
      case Meta.string  => Decoder.decodeString
      case Meta.char    => Decoder.decodeChar
      case Meta.int     => Decoder.decodeInt
      case Meta.long    => Decoder.decodeLong
      case Meta.integer => Decoder.decodeBigInt
      case Meta.float   => Decoder.decodeFloat
      case Meta.double  => Decoder.decodeDouble
      case Meta.decimal => Decoder.decodeBigDecimal
    }).asInstanceOf[Decoder[A]]

  def mappedAtomMetaCirceEncoder[A, B](implicit meta: Meta.MappedAtom[A, B]): Encoder[B] =
    simpleAtomMetaCirceEncoder(meta.meta).contramap(meta.g)

  def mappedAtomMetaCirceDecoder[A, B](implicit meta: Meta.MappedAtom[A, B]): Decoder[B] =
    simpleAtomMetaCirceDecoder(meta.meta).emapTry(meta.f(_).toTry)

  def atomMetaCirceEncoder[A](implicit meta: Meta.Atom[A]): Encoder[A] = meta match {
    case s: Meta.SimpleAtom[A]    => simpleAtomMetaCirceEncoder(s)
    case m: Meta.MappedAtom[_, A] => mappedAtomMetaCirceEncoder(m)
  }

  def atomMetaCirceDecoder[A](implicit meta: Meta.Atom[A]): Decoder[A] = meta match {
    case s: Meta.SimpleAtom[A]    => simpleAtomMetaCirceDecoder(s)
    case m: Meta.MappedAtom[_, A] => mappedAtomMetaCirceDecoder(m)
  }

  def optionMetaCirceEncoder[A](implicit meta: Meta.Option[A]): Encoder[Option[A]] =
    Encoder.encodeOption(metaCirceEncoder(meta.meta))

  def optionMetaCirceDecoder[A](implicit meta: Meta.Option[A]): Decoder[Option[A]] =
    Decoder.decodeOption(metaCirceDecoder(meta.meta))

  def listMetaCirceEncoder[L[_], A](implicit meta: Meta.List[L, A]): Encoder[L[A]] =
    Encoder.encodeIterable(metaCirceEncoder(meta.elementsMeta), meta.asIterable)

  def listMetaCirceDecoder[L[_], A](implicit meta: Meta.List[L, A]): Decoder[L[A]] =
    // For some reason Decoder.decodeIterable forces L to be iterable, which is unnecessary
    new BuilderDecoder[A, L](metaCirceDecoder(meta.elementsMeta)) {
      final protected def createBuilder(): mutable.Builder[A, L[A]] = meta.builder
    }

  @silent("TraversableOps")
  def unionMetaCirceDecoder[A](
    implicit meta: Meta.Union[A],
    unionCodecConfiguration: UnionCodecConfiguration[A]
  ): Decoder[A] =
    unionCodecConfiguration.getDiscriminator match {
      case Some(discriminatorReader) =>
        discriminatorReader.flatMap { discriminator =>
          meta.options
            .find(_.name == discriminator)
            .fold(Decoder.failed[A](DecodingFailure(s"Discriminator value $discriminator is invalid", List.empty)))(
              recordMetaCirceDecoder(_).widen[A]
            )
        }
      case None =>
        meta.options.map(metaCirceDecoder(_).widen[A]).reduceLeft(_ or _)
    }
}
