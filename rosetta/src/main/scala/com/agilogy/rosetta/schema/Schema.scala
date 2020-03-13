package com.agilogy.rosetta.schema

import scala.reflect.ClassTag

import cats.implicits._

sealed trait Schema {
  def name: String
}

sealed trait AttributeRequirement
object AttributeRequirement {
  case object Mandatory                    extends AttributeRequirement
  case object Optional                     extends AttributeRequirement
  final case class DefaultsTo[A](value: A) extends AttributeRequirement
}

object Schema {
  final case class AttributeSchema(name: String, attributeType: Schema, required: AttributeRequirement)
  final case class RecordSchema(name: String, attributes: List[AttributeSchema]) extends Schema
  def record(name: String, attributes: AttributeSchema*): Schema = RecordSchema(name, attributes.toList)
  final case class ListSchema(elementsSchema: Schema) extends Schema {
    override def name: String = s"List[${elementsSchema.name}]"
  }
  final case class AtomSchema(name: String) extends Schema

  def fromClass[A: ClassTag]: AtomSchema = {
    val classTagToString = implicitly[ClassTag[A]].toString()
    val lastDot          = classTagToString.lastIndexOf(".")
    AtomSchema(if (lastDot === -1) classTagToString else classTagToString.substring(lastDot + 1))
  }
}
