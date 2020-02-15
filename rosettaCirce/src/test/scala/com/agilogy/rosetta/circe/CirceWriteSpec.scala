package com.agilogy.rosetta.circe

import com.agilogy.rosetta.circe.CirceStringEngine.write
import com.agilogy.rosetta.circe.PersonWriteProtocol._

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

  test("get the schema of a write") {
    assertEquals(fooWrite.schema, Expected.fooSchema)
  }

}
