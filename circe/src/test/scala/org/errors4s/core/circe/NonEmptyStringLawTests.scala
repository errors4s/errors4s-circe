package org.errors4s.core.circe

import io.circe.testing._
import munit._
import org.errors4s.core._
import org.errors4s.core.cats.instances._
import org.errors4s.core.circe.instances._
import org.errors4s.core.scalacheck.instances._

final class NonEmptyStringLawTests extends DisciplineSuite with io.circe.testing.ArbitraryInstances {
  checkAll("NonEmptyString.CodecLaws", CodecTests[NonEmptyString].codec)
}
