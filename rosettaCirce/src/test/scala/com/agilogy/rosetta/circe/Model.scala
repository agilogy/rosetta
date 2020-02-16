package com.agilogy.rosetta.circe

final case class Age(value: Int)
final case class Person(
  name: String,
  age: Option[Age],
  favoriteColors: List[String] = List.empty,
  brothersAges: List[Age] = List.empty
)
final case class Department(name: String, head: Person)
final case class Foo(dept: Department)
