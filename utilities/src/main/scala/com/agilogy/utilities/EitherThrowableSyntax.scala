package com.agilogy.utilities

import com.github.ghik.silencer.silent

object EitherThrowableSyntax {

  implicit class EitherThrowableOps[A <: Throwable, B](self: Either[A, B]) {
    @silent("Throw")
    def getOrFail: B = self.fold(throw _, identity)
  }

}
