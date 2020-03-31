package com.agilogy.rosetta.circe

import CirceMetaProtocol._
import PersonMeta._
import io.circe.Encoder

import com.agilogy.rosetta.meta.Meta
import com.agilogy.rosetta.meta.syntax._

final case class SimpleExample(a: Int, b: String)

final class CirceMetaWriteSpec extends munit.FunSuite {

  implicit val simpleExampleMeta: Meta[SimpleExample] =
    Meta.Record2("simple", "a".mandatory[Int], "b".mandatory[String])(SimpleExample)(s => (s.a, s.b))

  def write[A: Encoder](value: A): String = Encoder[A].apply(value).noSpaces

  test("write a primitive as a wrapper class") {
    assertEquals(write(Age(5)), "5")
  }

  test("write a list of primitive attributes") {
    assertEquals(
      write(List("green", "blue")),
      """["green","blue"]"""
    )
  }

  test("write a list of mapped attributes") {
    assertEquals(
      write(List(Age(3), Age(7))),
      """[3,7]"""
    )
  }

  test("write an object of 2 fields") {
    assertEquals(
      write(SimpleExample(23, "a")),
      """{"a":23,"b":"a"}"""
    )
  }

  test("write an object") {
    assertEquals(
      write(Person("John", Option(Age(5)), List.empty, List.empty)),
      """{"name":"John","age":5,"favoriteColors":[],"brothersAges":[]}"""
    )
  }

  test("write an object with an optional attribute with value None") {
    assertEquals(
      write(Person("John", None, List.empty, List.empty)),
      """{"name":"John","age":null,"favoriteColors":[],"brothersAges":[]}"""
    )
  }

  test("write a list of primitive attributes in an object") {
    assertEquals(
      write(Person("Mary", Option(Age(5)), List("green", "blue"))),
      """{"name":"Mary","age":5,"favoriteColors":["green","blue"],"brothersAges":[]}"""
    )
  }

  test("write a list of mapped attributes in an object") {
    assertEquals(
      write(Person("Mary", Option(Age(5)), brothersAges = List(Age(3), Age(7)))),
      """{"name":"Mary","age":5,"favoriteColors":[],"brothersAges":[3,7]}"""
    )
  }

}
