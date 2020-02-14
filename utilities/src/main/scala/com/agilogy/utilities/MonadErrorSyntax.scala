package com.agilogy.utilities

import cats.MonadError
import cats.implicits._

object MonadErrorSyntax {
  implicit class MonadErrorMapErrorOps[A, E, F[_]](self: F[A])(implicit M: MonadError[F, E]) {
    def mapError(f: E => E): F[A] = self.adaptError { case x => f(x) }
  }
}
