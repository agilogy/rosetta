package com.agilogy.rosetta.circe

import com.github.ghik.silencer.silent
import io.circe.{ Decoder, Encoder, Json }

trait UnionCodecConfiguration[A] {
  def discriminate(discriminator: String): Encoder[A] => Encoder[A]
  def getDiscriminator: Option[Decoder[String]]
}

object UnionCodecConfiguration {

  def noDiscrimination[A]: UnionCodecConfiguration[A] = new UnionCodecConfiguration[A] {
    override def discriminate(discriminator: String): Encoder[A] => Encoder[A] = identity
    override def getDiscriminator: Option[Decoder[String]]                     = None
  }
  def structureDiscriminator[A]: UnionCodecConfiguration[A] = new UnionCodecConfiguration[A] {
    override def discriminate(discriminator: String): Encoder[A] => Encoder[A] =
      originalEncoder => Encoder.instance(x => Json.obj(discriminator -> originalEncoder(x)))

    override def getDiscriminator: Option[Decoder[String]] =
      Some(Decoder.decodeMap[String, Json].emap { m =>
        m.collectFirst {
          case (k: String, v) if v.isObject => k
        }.fold[Either[String, String]](Left("No discriminator found"))(Right(_))
      })
  }
  def attributeDiscriminator[A](discriminatorAttribute: String = "$type"): UnionCodecConfiguration[A] =
    new UnionCodecConfiguration[A] {
      override def discriminate(discriminator: String): Encoder[A] => Encoder[A] =
        originalEncoder =>
          Encoder.instance { x =>
            originalEncoder(x).deepMerge(Json.obj(discriminatorAttribute -> Json.fromString(discriminator)))
          }

      @silent("OptionPartial")
      override def getDiscriminator: Option[Decoder[String]] =
        Some(Decoder.decodeMap[String, Json].emap { m =>
          m.collectFirst {
            case (`discriminatorAttribute`, v) if v.isString => v.asString.get
          }.fold[Either[String, String]](Left("No discriminator found"))(Right(_))
        })
    }
}
