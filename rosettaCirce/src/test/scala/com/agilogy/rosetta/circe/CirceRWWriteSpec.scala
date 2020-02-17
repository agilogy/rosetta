package com.agilogy.rosetta.circe

import com.agilogy.rosetta.circe.PersonReadWriteProtocol._

final class CirceRWWriteSpec extends CirceWriteSpec {
  test("get the schema of a write") {
    assertEquals(fooWrite.schema, Expected.fooSchema)
  }

}
