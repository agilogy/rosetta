package io.circe

abstract class BuilderDecoder[A, C[_]](decodeA: Decoder[A]) extends SeqDecoder[A, C](decodeA)
