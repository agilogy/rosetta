package com.agilogy.rosetta.circe

import com.agilogy.rosetta.schema.Schema
import com.agilogy.rosetta.schema.Schema.{ AtomSchema, ListSchema }

object Expected {
  val stringSchema: AtomSchema = AtomSchema("String")
  val intSchema: AtomSchema    = AtomSchema("Int")
  val personSchema: Schema =
    Schema.record(
      "Person",
      "name"           -> stringSchema,
      "age"            -> intSchema,
      "favoriteColors" -> ListSchema(stringSchema),
      "brothersAges"   -> ListSchema(intSchema)
    )
  val departmentSchema: Schema = Schema.record("Department", "name" -> stringSchema, "head" -> personSchema)
  val fooSchema: Schema        = Schema.record("Foo", "dept"        -> departmentSchema)
}
