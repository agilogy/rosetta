package com.agilogy.rosetta.circe

import com.agilogy.rosetta.circe.PersonWriteProtocol._

final class CirceWriteWriteSpec extends CirceWriteSpec {

  test("get the schema of a write") {
    assertEquals(fooWrite.schema, Expected.withoutDefaultValues(Expected.fooSchema))
  }

}
