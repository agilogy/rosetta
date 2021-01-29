package com.agilogy.rosetta.circe

import cats.implicits._

import com.agilogy.rosetta.circe.CirceEngine.decode
import com.agilogy.rosetta.circe.CirceMetaProtocol._
import com.agilogy.rosetta.circe.ModelMeta._
import com.agilogy.rosetta.read.{ ReadError, Segment }

final class CirceMetaReadSpec extends munit.FunSuite {

  test("read a primitive as a wrapper class") {
    assertEquals(decode[Age]("5"), Age(5).asRight[ReadError])
  }

  test("fail to read a primitive as a wrapper class when its the wrong type") {
    assertEquals(decode[Age]("false"), (ReadError.wrongType("Int"): ReadError).asLeft[Age])
  }

  test("read a list of primitive values") {
    assertEquals(
      decode[List[String]]("""["green", "blue"]"""),
      List("green", "blue").asRight[ReadError]
    )
  }

  test("read a list of mapped values") {
    assertEquals(
      decode[List[Age]]("""[3, 7]"""),
      List(Age(3), Age(7)).asRight[ReadError]
    )
  }

  test("read a map") {
    assertEquals(
      decode[Map[String, String]]("""{"foo":"1", "bar": "2"}"""),
      Map("foo" -> "1", "bar" -> "2").asRight[ReadError]
    )
  }

  test("read an object") {
    assertEquals(
      decode[Person](
        """{"name":"John", "age":5, "favoriteColors":[], "brothersAges":[],"custom":{"foo":"1", "bar": "2"}}"""
      ),
      Person("John", Option(Age(5)), List.empty, List.empty, Map("foo" -> Age(1), "bar" -> Age(2))).asRight[ReadError]
    )
  }

  test("read an object with default attribute when they are not in the json") {
    assertEquals(
      decode[Person]("""{"name":"John", "age":5}"""),
      Person("John", Option(Age(5)), List.empty, List.empty).asRight[ReadError]
    )
  }

  test("read instances of a union meta") {
    val value = """[{"car":{"brand":"Volkswagen","model":"Beatle"}},{"bicycle":{"color":"blue"}}]"""
    assertEquals(
      decode[List[Vehicle]](value),
      List(Car("Volkswagen", "Beatle"), Bicycle("blue")).asRight[ReadError]
    )
  }

  val wrongPerson = """{"age":"young","favoriteColors":3, "brothersAges":[]}"""
  val wrongPersonErrors: ReadError =
    ReadError.MissingAttributeError.at("name") ++
      ReadError.WrongTypeReadError("Int").at("age") ++
      ReadError.WrongTypeReadError("Array").at("favoriteColors")

  test("fail to read an object and accumulate errors") {
    assertEquals(decode[Person](wrongPerson), wrongPersonErrors.asLeft[Person])
  }

  test("fail to read an object when it is not one") {
    val res = decode[Foo]("""{"dept":{"name":"a","head": 1}}""")
    println(res)
    assertEquals(
      res,
      (ReadError.MissingAttributeError.at(Segment("dept"), Segment("head"), Segment("name")) ++
        ReadError.wrongType("Object").at(Segment("dept"), Segment("head")))
        .asLeft[Foo]
    )
  }

  test("fail to read an optional attribute when it is wrong") {
    assertEquals(
      decode[Person]("""{"name":"John", "age":"young"}"""),
      ReadError.wrongType("Int").at("age").asLeft[Person]
    )
  }

  test("fail to read a wrong object inside an object and accumulate errors") {
    assertEquals(
      decode[Foo](s"""{"dept":{"name": "Foo", "head":$wrongPerson}}"""),
      wrongPersonErrors.at(Segment("dept"), Segment("head")).asLeft[Foo]
    )
  }

  test("read a list of primitive attributes") {
    assertEquals(
      decode[Person]("""{"name":"Mary", "age":5, "favoriteColors":["green", "blue"]}"""),
      Person("Mary", Option(Age(5)), List("green", "blue")).asRight[ReadError]
    )
  }

  test("read a list of mapped attributes") {
    assertEquals(
      decode[Person]("""{"name":"Mary", "age":5, "brothersAges":[3, 7]}"""),
      Person("Mary", Option(Age(5)), brothersAges = List(Age(3), Age(7))).asRight[ReadError]
    )
  }

  test("handle errors when reading an array") {
    val res = decode[Person]("""{"name":"Mary", "favoriteColors":[false, "blue", 3]}""")
    println(res)
    assertEquals(
      res,
      (ReadError.wrongType("String").at(Segment("favoriteColors"), Segment(0)) ++
        ReadError.wrongType("String").at(Segment("favoriteColors"), Segment(2))).asLeft[Person]
    )
  }

//  test("get the schema of a read") {
//    assertEquals(fooRead.schema, Expected.fooSchema)
//  }
}
