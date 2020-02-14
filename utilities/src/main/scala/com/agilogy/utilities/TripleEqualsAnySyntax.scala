package com.agilogy.utilities

import com.github.ghik.silencer.silent

trait TripleEqualsAnySyntax {

  implicit class TripleEqualsOps[A](leftSide: A) {
    @silent("parameter value ev in method === is never used")
    @SuppressWarnings(Array("scalafix:DisableSyntax.=="))
    def ===[B](rightSide: B)(implicit ev: A =:= B): Boolean = leftSide == rightSide
  }

}

object TripleEqualsAnySyntax extends TripleEqualsAnySyntax
