package com.agilogy.rosetta.circe

import com.github.ghik.silencer.silent

import com.agilogy.rosetta.schema.AttributeRequirement.{ DefaultsTo, Mandatory, Optional }
import com.agilogy.rosetta.schema.Schema
import com.agilogy.rosetta.schema.Schema.{ AtomSchema, AttributeSchema, ListSchema }

object Expected {
  val stringSchema: AtomSchema = AtomSchema("String")
  val intSchema: AtomSchema    = AtomSchema("Int")
  val personSchema: Schema =
    Schema.record(
      "Person",
      AttributeSchema("name", stringSchema, Mandatory),
      AttributeSchema("age", intSchema, Optional),
      AttributeSchema("favoriteColors", ListSchema(stringSchema), DefaultsTo(Nil)),
      AttributeSchema("brothersAges", ListSchema(intSchema), DefaultsTo(Nil))
    )
  val departmentSchema: Schema =
    Schema.record(
      "Department",
      AttributeSchema("name", stringSchema, Mandatory),
      AttributeSchema("head", personSchema, Mandatory)
    )
  val fooSchema: Schema = Schema.record("Foo", AttributeSchema("dept", departmentSchema, Mandatory))

  @silent("Recursion")
  def withoutDefaultValues(s: Schema): Schema = s match {
    case Schema.RecordSchema(name, attributes) =>
      Schema.RecordSchema(
        name,
        attributes.map {
          case AttributeSchema(n, s, DefaultsTo(_)) => AttributeSchema(n, withoutDefaultValues(s), Mandatory)
          case AttributeSchema(n, s, r)             => AttributeSchema(n, withoutDefaultValues(s), r)
        }
      )
    case Schema.AtomSchema(n) => Schema.AtomSchema(n)
    case Schema.ListSchema(s) => Schema.ListSchema(withoutDefaultValues(s))
  }
}
