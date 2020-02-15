package com.agilogy.rosetta.schema

import scala.reflect.ClassTag

import cats.implicits._

sealed trait Schema

object Schema {
  final case class RecordSchema(name: String, attributes: List[(String, Schema)]) extends Schema
  def record(name: String, attributes: (String, Schema)*): Schema = RecordSchema(name, attributes.toList)
  final case class ListSchema(elementsSchema: Schema) extends Schema
  final case class AtomSchema(name: String)           extends Schema

  def fromClass[A: ClassTag]: AtomSchema = {
    val classTagToString = implicitly[ClassTag[A]].toString()
    val lastDot          = classTagToString.lastIndexOf(".")
    AtomSchema(if (lastDot === -1) classTagToString else classTagToString.substring(lastDot + 1))
  }
}
