package com.agilogy.rosetta.circe

import scala.collection.mutable

import cats.implicits._

import com.github.ghik.silencer.silent
import io.circe.{ BuilderDecoder, Decoder, DecodingFailure, Encoder }

import com.agilogy.rosetta.meta.Meta

object CirceMetaProtocol extends TemplatedCirceEncoders with TemplatedCirceDecoders {

  @silent("AsInstanceOf")
  implicit def metaEncoder[A](
    implicit meta: Meta[A],
    unionCodecConfiguration: UnionCodecConfiguration[A] = UnionCodecConfiguration.structureDiscriminator[A]
  ): Encoder[A] =
    (meta match {
      case s: Meta.SimpleAtom[A]    => simpleAtomMetaEncoder(s)
      case m: Meta.MappedAtom[_, A] => mappedAtomMetaEncoder(m)
      case o: Meta.Option[_]        => optionMetaEncoder(o)
      case l: Meta.List[_, _]       => listMetaEncoder(l)
      case r: Meta.Record[A]        => recordMetaEncoder(r)
      case u: Meta.Union[A]         => unionMetaEncoder(u, unionCodecConfiguration)
    }).asInstanceOf[Encoder[A]]

  @silent("AsInstanceOf")
  implicit def metaDecoder[A](
    implicit meta: Meta[A],
    unionCodecConfiguration: UnionCodecConfiguration[A] = UnionCodecConfiguration.structureDiscriminator[A]
  ): Decoder[A] =
    (meta match {
      case s: Meta.SimpleAtom[A]    => simpleAtomMetaDecoder(s)
      case m: Meta.MappedAtom[_, A] => mappedAtomMetaDecoder(m)
      case o: Meta.Option[_]        => optionMetaDecoder(o)
      case l: Meta.List[_, _]       => listMetaDecoder(l)
      case r: Meta.Record[A]        => recordMetaDecoder(r)
      case u: Meta.Union[A]         => unionMetaDecoder(u, unionCodecConfiguration)
    }).asInstanceOf[Decoder[A]]

  @silent("AsInstanceOf")
  def simpleAtomMetaEncoder[A](implicit meta: Meta.SimpleAtom[A]): Encoder[A] =
    (meta match {
      case Meta.unit    => Encoder.encodeUnit
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
  def simpleAtomMetaDecoder[A](implicit meta: Meta.SimpleAtom[A]): Decoder[A] =
    (meta match {
      case Meta.unit    => Decoder.decodeUnit
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

  def mappedAtomMetaEncoder[A, B](implicit meta: Meta.MappedAtom[A, B]): Encoder[B] =
    simpleAtomMetaEncoder(meta.meta).contramap(meta.g)

  def mappedAtomMetaDecoder[A, B](implicit meta: Meta.MappedAtom[A, B]): Decoder[B] =
    simpleAtomMetaDecoder(meta.meta)
      .emap(meta.f(_).leftMap(e => s"Error decoding mapped atom ${meta.name}: ${e.getMessage}"))

  def optionMetaEncoder[A](implicit meta: Meta.Option[A]): Encoder[Option[A]] =
    Encoder.encodeOption(metaEncoder(meta.meta))

  def optionMetaDecoder[A](implicit meta: Meta.Option[A]): Decoder[Option[A]] =
    Decoder.decodeOption(metaDecoder(meta.meta))

  def listMetaEncoder[L[_], A](implicit meta: Meta.List[L, A]): Encoder[L[A]] =
    Encoder.encodeIterable(metaEncoder(meta.elementsMeta), meta.asIterable)

  def listMetaDecoder[L[_], A](implicit meta: Meta.List[L, A]): Decoder[L[A]] =
    // For some reason Decoder.decodeIterable forces L to be iterable, which is unnecessary
    new BuilderDecoder[A, L](metaDecoder(meta.elementsMeta)) {
      final protected def createBuilder(): mutable.Builder[A, L[A]] = meta.builder()
    }

  @silent("TraversableOps")
  def unionMetaDecoder[A](
    implicit meta: Meta.Union[A],
    unionCodecConfiguration: UnionCodecConfiguration[A]
  ): Decoder[A] =
    unionCodecConfiguration.getDiscriminator match {
      case Some(discriminatorReader) =>
        discriminatorReader.flatMap { discriminator =>
          meta.options
            .find(_.name === discriminator)
            .fold(Decoder.failed[A](DecodingFailure(s"Discriminator value $discriminator is invalid", List.empty))) {
              d =>
                Decoder.forProduct1[A, A](discriminator)(identity)(recordMetaDecoder(d).widen[A])
            }
        }
      case None =>
        meta.options.map(metaDecoder(_).widen[A]).reduceLeft(_ or _)
    }
}
