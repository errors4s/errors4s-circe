# Errors For Scala (Core) Circe Instances #

# ScalaDoc #

[The Scaladoc for errors4s-core-circe may be viewed here][javadoc].

[javadoc]: https://www.javadoc.io/doc/org.errors4s/errors4s-core-circe_2.13/1.0.0.0-RC0/index.html "Scaladoc"

# Overview #

This project provides [circe][circe] typeclass instances for [errors4s-core][errors4s-core] types.

[circe]: https://github.com/circe/circe "Circe"
[errors4s-core]: https://github.com/errors4s/errors4s-core "Errors4s Core"

## Using ##

Add this to your `libraryDependencies` in your `build.sbt`.

```scala
    "org.errors4s" %% "errors4s-core-circe" % "1.0.0.0-RC0"
```

```scala
import io.circe._
import io.circe.syntax._
import org.errors4s.core._
import org.errors4s.core.circe.instances._

val nes: NonEmptyString = NonEmptyString("A nonempty string")
// nes: NonEmptyString = A nonempty string

val json: Json = nes.asJson
// json: Json = "A nonempty string"

val decodedNes: Decoder.Result[NonEmptyString] = Decoder[NonEmptyString].decodeJson(json)
// decodedNes: Decoder.Result[NonEmptyString] = Right(A nonempty string)

decodedNes == Right(nes)
// res0: Boolean = true
```

# Version Support Matrix #

This project uses [Package Versioning Policy (PVP)][pvp]. This is to allow long term support (see [this discussion][errors4s-core-pvp]). This table lists the currently supported, upcoming, and recently end of life versions.

If you need support for a version combination which is not listed here, please open an issue and we will endeavor to add support for it if possible.

|Version|Errors4s Core|Circe Version|Scala 2.11|Scala 2.12|Scala 2.13|Scala 3.0|Supported       |
|-------|:-----------:|:-----------:|:--------:|:--------:|:--------:|:-------:|:--------------:|
|2.0.x.x|1.0.x.x      |0.14.x       |No        |Yes       |Yes       |Yes      |Not Yet Released|
|1.0.x.x|1.0.x.x      |0.13.x       |No        |Yes       |Yes       |Yes      |Yes             |

[pvp]: https://pvp.haskell.org/ "PVP"
[errors4s-core-pvp]: https://github.com/errors4s/errors4s-core#versioning "Errors4s Core: Versioning"
[semver]: https://semver.org/ "Semver"
