package com.agilogy.rosetta.circe

import com.agilogy.rosetta.meta.Meta
import com.agilogy.rosetta.meta.syntax._

object PersonMeta {

  implicit val ageMeta: Meta.MappedAtom[Int, Age] = Meta.int.imap("age")(Age)(_.value)
  implicit val personMeta: Meta.Record4[String, Option[Age], List[String], List[Age], Person] =
    Meta.Record4(
      "person",
      "name".mandatory[String],
      "age".optional[Age],
      "favoriteColors".defaultTo(List.empty[String]),
      "brothersAges".defaultTo(List.empty[Age])
    )(Person)(
      p => (p.name, p.age, p.favoriteColors, p.brothersAges)
    )
  implicit val departmentMeta: Meta.Record2[String, Person, Department] =
    Meta.Record2("department", "name".mandatory[String], "head".mandatory[Person])(Department)(d => (d.name, d.head))
  implicit val fooMeta: Meta.Record1[Department, Foo] = Meta.Record1("foo", "dept".mandatory[Department])(Foo)(_.dept)

  implicit class CastOps[A, B](self: A => Option[Any]) extends (A => Option[A]) {
    override def apply(v1: A): Option[A] = self(v1).map(_ => v1)
  }

}
