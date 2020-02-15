package com.agilogy.rosetta.circe

import cats.implicits._

import com.agilogy.rosetta.circe.CirceStringEngine._
import com.agilogy.rosetta.protocol.Protocol

object PersonWriteProtocol extends Protocol(CirceStringEngine) {

  implicit val ageReads: W[Age] = W[Int].contramap(_.value)
  implicit val personWrites: W[Person] =
    (
      "name".write[String],
      "age".write[Age],
      "favoriteColors".write[List[String]],
      "brothersAges".write[List[Age]]
    ).contramapN[Person](p => (p.name, p.age, p.favoriteColors, p.brothersAges)).apply("Person")

  implicit val departmentWrite: W[Department] =
    ("name".write[String], "head".write[Person]).contramapN[Department](d => (d.name, d.head)).apply("Department")
  implicit val fooWrite: W[Foo] = "dept".write[Department].contramap[Foo](_.dept).apply("Foo")

}
