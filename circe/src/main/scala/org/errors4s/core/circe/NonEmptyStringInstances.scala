package org.errors4s.core.circe

import io.circe._
import org.errors4s.core._

private[circe] trait NonEmptyStringInstances {

  implicit final val nesCodec: Codec[NonEmptyString] = Codec
    .from(Decoder[String].emap(NonEmptyString.from), Encoder[String].contramap(_.value))

  implicit final val keyDecoder: KeyDecoder[NonEmptyString] = KeyDecoder
    .instance[NonEmptyString](value => NonEmptyString.from(value).toOption)

  implicit final val keyEncoder: KeyEncoder[NonEmptyString] = KeyEncoder[String].contramap(_.value)
}
