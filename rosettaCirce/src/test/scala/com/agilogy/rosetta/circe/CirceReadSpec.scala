package com.agilogy.rosetta.circe

import cats.implicits._

import com.agilogy.rosetta.circe.CirceStringEngine.read
import com.agilogy.rosetta.circe.PersonReadProtocol._
import com.agilogy.rosetta.read.ReadErrorCause.NativeReadError
import com.agilogy.rosetta.read.{ ReadError, Segment }

class CirceReadSpec extends munit.FunSuite {

  test("read a primitive as a wrapper class") {
    assertEquals(read[Age]("5"), Age(5).asRight[ReadError])
  }

  test("fail to read a primitive of the wrong type as a wrapper class") {
    assertEquals(read[Age]("false"), ReadError(NativeReadError("Int"), List()).asLeft[Age])
  }

  test("read an object") {
    assertEquals(
      read[Person]("""{"name":"John", "age":5}"""),
      Person("John", Age(5), List.empty, List.empty).asRight[ReadError]
    )
  }

  // TODO: Maybe we want to optionally accumulate errors?
  test("fail to read an object and give information about the first error found") {
    assertEquals(
      read[Person]("""{"name":false}"""),
      ReadError(NativeReadError("String"), List(Segment.Attribute("name"))).asLeft[Person]
    )
  }

  test("fail to read an object inside an object") {
    assertEquals(
      read[Foo]("""{"dept":{"name": "Foo", "head":{"name":3, "age":5}}}"""),
      ReadError(
        NativeReadError("String"),
        List(Segment.Attribute("dept"), Segment.Attribute("head"), Segment.Attribute("name"))
      ).asLeft[Foo]
    )
  }

  test("read a list of primitive attributes") {
    assertEquals(
      read[Person]("""{"name":"Mary", "age":5, "favoriteColors":["green", "blue"]}"""),
      Person("Mary", Age(5), List("green", "blue")).asRight[ReadError]
    )
  }

  test("read a list of mapped attributes") {
    assertEquals(
      read[Person]("""{"name":"Mary", "age":5, "brothersAges":[3, 7]}"""),
      Person("Mary", Age(5), brothersAges = List(Age(3), Age(7))).asRight[ReadError]
    )
  }

  test("read a list of mapped attributes with errors") {
    assertEquals(
      read[Person]("""{"name":"Mary", "age":5, "brothersAges":[3, false]}"""),
      ReadError(NativeReadError("Int"), List(Segment.Attribute("brothersAges"), Segment.ArrayElement(1)))
        .asLeft[Person]
    )
  }

  test("get the schema of a read") {
    assertEquals(fooRead.schema, Expected.fooSchema)
  }
}
