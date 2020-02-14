package com.agilogy.utilities

import com.github.ghik.silencer.silent

trait ValidatedCompanion[A, B, E <: Throwable] {

  @silent("throw")
  final def unsafe(value: A): B = apply(value).fold(throw _, identity)

  def apply(value: A): Either[E, B]
}

trait StringValidatedCompanion[A, E <: Throwable] extends ValidatedCompanion[String, A, E]
