package com.agilogy.rosetta.circe

import com.agilogy.rosetta.meta.Meta
import com.agilogy.rosetta.meta.syntax._

object PersonMeta {

  implicit val ageMeta: Meta[Age] = Meta.int.imap("age")(Age)(_.value)

  implicit val personMeta: Meta[Person] =
    Meta.record(
      "person",
      "name".mandatory[String],
      "age".optional[Age],
      "favoriteColors".defaultTo(List.empty[String]),
      "brothersAges".defaultTo(List.empty[Age])
    )(Person)(Person.unapply)

  implicit val departmentMeta: Meta[Department] =
    Meta.record("department", "name".mandatory[String], "head".mandatory[Person])(Department)(Department.unapply)

  implicit val fooMeta: Meta[Foo] = Meta.record("foo", "dept".mandatory[Department])(Foo)(Foo.unapply)

}
