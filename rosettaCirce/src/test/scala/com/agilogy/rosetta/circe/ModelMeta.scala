package com.agilogy.rosetta.circe

import scala.reflect.ClassTag

import com.agilogy.rosetta.meta.Meta
import com.agilogy.rosetta.meta.syntax._

object ModelMeta {

  implicit val ageMeta: Meta[Age] = Meta.int.imap("age")(Age)(_.value)

  implicit val personMeta: Meta[Person] =
    Meta.record(
      "person",
      "name".mandatory[String],
      "age".optional[Age],
      "favoriteColors".defaultTo(List.empty[String]),
      "brothersAges".defaultTo(List.empty[Age]),
      "custom".defaultTo(Map.empty[String, Age])
    )(Person)(Person.unapply)

  implicit val departmentMeta: Meta[Department] =
    Meta.record("department", "name".mandatory[String], "head".mandatory[Person])(Department)(Department.unapply)

  implicit val fooMeta: Meta[Foo] = Meta.record("foo", "dept".mandatory[Department])(Foo)(Foo.unapply)

  def cast[A](value: Any)(implicit classTag: ClassTag[A]): Option[A] = value match {
    case a: A => Some(a)
    case _    => None
  }

  private implicit val carMeta: Meta.Record[Car] =
    Meta.record("car", "brand".mandatory[String], "model".mandatory[String])(Car)(Car.unapply)
  private implicit val bicycleMeta: Meta.Record[Bicycle] =
    Meta.record("bicycle", "color".mandatory[String])(Bicycle)(Bicycle.unapply)

  implicit val vehicleMeta: Meta[Vehicle] = Meta.Union2[Vehicle, Car, Bicycle]("vehicle")

}
