package com.agilogy.rosetta.circe

import com.agilogy.rosetta.circe.CirceStringEngine.write
import com.agilogy.rosetta.circe.PersonWriteProtocol._
import com.agilogy.rosetta.schema.Schema
import com.agilogy.rosetta.schema.Schema.{ AtomSchema, ListSchema }

class CirceWriteSpec extends munit.FunSuite {

  test("write a primitive as a wrapper class") {
    assertEquals(write(Age(5)), "5")
  }

  test("write an object") {
    assertEquals(
      write(Person("John", Age(5), List.empty, List.empty)),
      """{"name":"John","age":5,"favoriteColors":[],"brothersAges":[]}"""
    )
  }

  test("write a list of primitive attributes") {
    assertEquals(
      write(Person("Mary", Age(5), List("green", "blue"))),
      """{"name":"Mary","age":5,"favoriteColors":["green","blue"],"brothersAges":[]}"""
    )
  }

  test("write a list of mapped attributes") {
    assertEquals(
      write(Person("Mary", Age(5), brothersAges = List(Age(3), Age(7)))),
      """{"name":"Mary","age":5,"favoriteColors":[],"brothersAges":[3,7]}"""
    )
  }

  test("get the schema of a writes") {
    val stringSchema = AtomSchema("String")
    val intSchema    = AtomSchema("Int")
    val personSchema =
      Schema.record(
        "Person",
        "name"           -> stringSchema,
        "age"            -> intSchema,
        "favoriteColors" -> ListSchema(stringSchema),
        "brothersAges"   -> ListSchema(intSchema)
      )
    val departmentSchema = Schema.record("Department", "name" -> stringSchema, "head" -> personSchema)
    assertEquals(fooWrite.schema, Schema.record("Foo", "dept" -> departmentSchema))
  }

}
