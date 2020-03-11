package com.agilogy.rosetta.circe

import cats.implicits._

import com.github.ghik.silencer.silent

import com.agilogy.rosetta.circe.CirceStringEngine.{ read, R }
import com.agilogy.rosetta.read.ReadError

@silent("ImplicitParameter")
abstract class CirceReadSpec(implicit ageRead: R[Age], personRead: R[Person], fooRead: R[Foo]) extends munit.FunSuite {

  test("read a primitive as a wrapper class") {
    assertEquals(read[Age]("5"), Age(5).asRight[ReadError])
  }

  test("fail to read a primitive of the wrong type as a wrapper class") {
    assertEquals(read[Age]("false"), ReadError("Int expected").asLeft[Age])
  }

  test("read an object") {
    assertEquals(
      read[Person]("""{"name":"John", "age":5}"""),
      Person("John", Option(Age(5)), List.empty, List.empty).asRight[ReadError]
    )
  }

  val wrongPerson = """{"name":false,"age":"young","favoriteColors":3}"""
  val wrongPersonErrors: ReadError = ReadError.ofRecord(
    "name"           -> ReadError.SimpleMessageReadError("String expected"),
    "age"            -> ReadError.SimpleMessageReadError("Int expected"),
    "favoriteColors" -> ReadError.SimpleMessageReadError("Array expected")
  )

  test("fail to read an object and accumulate errors") {
    assertEquals(read[Person](wrongPerson), wrongPersonErrors.asLeft[Person])
  }

  test("fail to read an object when it is not one") {
    assertEquals(
      read[Foo]("""{"dept":{"name":"a","head": 1}}"""),
      ReadError.ofRecord("dept" -> ReadError.ofRecord("head" -> ReadError.atomic("Object expected"))).asLeft[Foo]
    )
  }

  test("fail to read an optional attribute when it is wrong") {
    assertEquals(
      read[Person]("""{"name":"John", "age":"young"}"""),
      ReadError.ofRecord("age" -> ReadError.atomic("Int expected")).asLeft[Person]
    )

  }

  test("fail to read a wrong object inside an object and accumulate errors") {
    assertEquals(
      read[Foo](s"""{"dept":{"name": "Foo", "head":$wrongPerson}}"""),
      ReadError.ofRecord("dept" -> ReadError.ofRecord("head" -> wrongPersonErrors)).asLeft[Foo]
    )
  }

  test("read a list of primitive attributes") {
    assertEquals(
      read[Person]("""{"name":"Mary", "age":5, "favoriteColors":["green", "blue"]}"""),
      Person("Mary", Option(Age(5)), List("green", "blue")).asRight[ReadError]
    )
  }

  test("read a list of mapped attributes") {
    assertEquals(
      read[Person]("""{"name":"Mary", "age":5, "brothersAges":[3, 7]}"""),
      Person("Mary", Option(Age(5)), brothersAges = List(Age(3), Age(7))).asRight[ReadError]
    )
  }

  test("handle errors when reading an array") {
    assertEquals(
      read[Person]("""{"name":"Mary", "favoriteColors":[false, "blue", 3]}"""),
      ReadError
        .ofRecord(
          "favoriteColors" -> ReadError.ofArray(0 -> ReadError("String expected"), 2 -> ReadError("String expected"))
        )
        .asLeft[Person]
    )
  }

  test("get the schema of a read") {
    assertEquals(fooRead.schema, Expected.fooSchema)
  }
}
