package com.agilogy.rosetta.circe

import com.agilogy.rosetta.meta.Meta
import io.circe.{ Decoder, Encoder }

object CirceMetaProtocol extends RecordCirceEncoders with RecordCirceDecoders {

  implicit def mappedAtomCirceEncoder[A, B](implicit meta: Meta.MappedAtom[A, B], e: Encoder[A]): Encoder[B] =
    Encoder[A].contramap(meta.g)

  implicit def mappedAtomCirceDecoder[A, B](implicit meta: Meta.MappedAtom[A, B], d: Decoder[A]): Decoder[B] =
    Decoder[A].emapTry(meta.f(_).toTry)

}
