package com.agilogy.rosetta.meta

import com.agilogy.rosetta.meta.Meta.Attribute

object syntax {
  final implicit class AttributeStringSyntax(val self: String) extends AnyVal {
    def optional[A: Meta]: Attribute[Option[A]]        = Attribute.Mandatory(self)
    def mandatory[A: Meta]: Attribute[A]               = Attribute.Mandatory(self)
    def defaultTo[A: Meta](defaultTo: A): Attribute[A] = Attribute.WithDefault(self, defaultTo)
  }
}
