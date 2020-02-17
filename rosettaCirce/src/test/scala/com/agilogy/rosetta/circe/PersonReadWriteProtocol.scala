package com.agilogy.rosetta.circe

import cats.implicits._

import com.agilogy.rosetta.circe.CirceStringEngine._
import com.agilogy.rosetta.protocol.Protocol

object PersonReadWriteProtocol extends Protocol(CirceStringEngine) {

  implicit val ageReads: RW[Age] = RW[Int].imap(Age)(_.value)
  implicit val personWrites: RW[Person] =
    (
      "name".rw[String],
      "age".rwOpt[Age],
      "favoriteColors".rwOr(List.empty[String]),
      "brothersAges".rwOr(List.empty[Age])
    ).imapN(Person)(p => (p.name, p.age, p.favoriteColors, p.brothersAges)).apply("Person")

  implicit val departmentWrite: RW[Department] =
    ("name".rw[String], "head".rw[Person]).imapN(Department)(d => (d.name, d.head)).apply("Department")
  implicit val fooWrite: RW[Foo] = "dept".rw[Department].imap(Foo)(_.dept).apply("Foo")

}
