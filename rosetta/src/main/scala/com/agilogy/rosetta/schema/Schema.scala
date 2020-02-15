package com.agilogy.rosetta.schema

sealed trait Schema

object Schema {
  final case class RecordSchema(name: String, attributes: List[(String, Schema)]) extends Schema
  def record(name: String, attributes: (String, Schema)*): Schema = RecordSchema(name, attributes.toList)
  final case class ListSchema(elementsSchema: Schema) extends Schema
  final case class AtomSchema(name: String)           extends Schema
}
