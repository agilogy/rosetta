package com.agilogy.rosetta.circe

import cats.implicits._

import CirceStringEngine._

import com.agilogy.rosetta.protocol.Protocol

object PersonReadProtocol extends Protocol(CirceStringEngine) {

  implicit val ageReads: R[Age] = R[Int].map(Age.apply)
  implicit val personReads: R[Person] =
    (
      "name".read[String],
      "age".read[Age],
      "favoriteColors".readOr[List[String]](List.empty),
      "brothersAges".readOr[List[Age]](List.empty)
    ).mapN(Person.apply).apply("Person")

  implicit val departmentRead: R[Department] =
    ("name".read[String], "head".read[Person]).mapN(Department.apply).apply("Department")
  implicit val fooRead: R[Foo] = "dept".read[Department].map(Foo.apply).apply("Foo")

}
