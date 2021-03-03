# Errors For Scala #

This project has the following goals,

* Provide a better base type for errors than `Throwable`.
* Provide an implementation of [RFC 7807][rfc-7807]

# ScalaDoc #

[The unified ScalaDoc may be viewed here][javadoc].

[javadoc]: https://www.javadoc.io/doc/io.isomarcte/errors4s_2.13/latest/index.html "Scaladoc"

# Version Information #

As of version 0.2.0.0, this project uses [Package Versioning Policy][pvp]. In addition to the standard PVP constraints, updates to the A version indicate a change to the dependency set which is known to binary incompatible and updates to the B version will indicate an internal binary incompatibility, e.g. a removed symbol.

[pvp]: https://pvp.haskell.org/ "PVP"

# Overview #

This project provides a better root error type `Error` which extends `Throwable` but provides better minimum constraints on what must be declared with the error. The `core` module has one  dependencies, [refined][refined]. [refined][refined] is used to enforce the fundamental concept of `errors4s`, when throwing an error you must provide enough information to make that error _useful_.

# Why #

Before we discuss the details of this project, it might be helpful for a moment to motivate its existence.

In both OO and FP Scala code, it is common to use `Throwable` as the root type for errors. This is convenient as it allows for simple interoperability with existing JVM/Java code. `Throwable` however has several less than helpful constructions. For example,

```scala
val t: Throwable = new RuntimeException()
// t: Throwable = java.lang.RuntimeException
t.getMessage
// res0: String = null
t.getCause
// res1: Throwable = null
```

The very last thing that you want to happen when you are debugging an error is to be given absolutely no information about. Okay, so this is clearly a contrived example, but this does happen in the real world. Here is a bit more realistic of a motivating example. Suppose we are using a library for parsing which and we are adapting the error to our a new error type.

```scala
// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

import scala.util.Try

case class OurException(message: String) extends RuntimeException(message)

def add(a: String, b: String): Either[OurException, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(OurException(e.getMessage)),
        i => Right(i)
    )

add("1", "a")
// res2: Either[OurException, Int] = Left(value = OurException(message = null))
```

In this example the library intends to indicate the type of the error in the class name of the thrown exception, thus it uses `null` as the message. This is a not uncommon idiom in some codebases. Unfortunately our code is adapting the error to a new exception type, another common idiom, and it is relying on the value of `getMessage` from the underlying to indicate information about the error. At the end of the day this leaves us with the useless and infuriating message `null`.

# Modules #

## Core ##

The core module defines a new type `Error`. It extends `RuntimeException` (and thus `Throwable`), but provides stronger constraints on the required information. It is an open `trait` so new domain specific error types are free to extend it (as is the common idiom with `RuntimeException`). The fundamental member of `Error` is `primaryErrorMessage`, a [refined][refined] `NonEmptyString`. For example, the following code will not compile.

```scala
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

Error.withMessage(null: NonEmptyString)
// error: type mismatch;
//  found   : Null(null)
//  required: eu.timepit.refined.types.all.NonEmptyString
//     (which expands to)  eu.timepit.refined.api.Refined[String,eu.timepit.refined.boolean.Not[eu.timepit.refined.collection.Empty]]
// Error.withMessage(null: NonEmptyString)
//                   ^^^^
```

Nor will this code,

```scala
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

Error.withMessage(NonEmptyString(""))
```

but this code does compile,

```scala
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

val e: Error = Error.withMessage(NonEmptyString("Failure During Parsing"))
// e: Error = ErrorImpl(
//   primaryErrorMessage = Failure During Parsing,
//   secondaryErrorMessages = Vector(),
//   causes = Vector()
// )
```

`primaryErrorMessage` represents the unchanging context of the error. In order to generate a `NonEmptyString` (as opposed to an `Either[String, NonEmptyString]`) at compile time you have to provide a literal `String` value, e.g. `"Failure During Parsing"`. An interpolated value will not work, e.g. `s"Parsing failure: ${value}"`. For providing more context specific information about the error you should use the `secondaryErrorMessages: List[String]` field.

Since `Error` extends `Throwable` we can interoperate with code which expects `Throwable` with no issues.

```scala
def adaptError(t: Throwable): RuntimeException =
  new RuntimeException(t.getMessage)

adaptError(e)
// res4: RuntimeException = java.lang.RuntimeException: Failure During Parsing
```

`Error` also provides a built in method to attempt to handle situations where the class name of some arbitrary `Throwable` was intended to communicate why an error occurred. Going back to our original example, we can use `Error.fromThrowable` to get a much more useful error.

```scala
import io.isomarcte.errors4s.core._
import scala.util.Try

// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

def add(a: String, b: String): Either[Throwable, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(Error.fromThrowable(e)),
        i => Right(i)
    )

add("1", "a")
// res6: Either[Throwable, Int] = Left(
//   value = ErrorImpl(
//     primaryErrorMessage = repl.MdocSession.App5.InvalidIntException,
//     secondaryErrorMessages = Vector(),
//     causes = Vector(repl.MdocSession$App5$InvalidIntException)
//   )
// )
```

You can see that since the `InvalidIntException` didn't have a defined `getMessage` `Error.fromThrowable` did inspection on the class name as a fallback method of generating an error message. Note, `errors4s` does _not recommended_ using this in general. A better approach would be to use `Error.withMessageAndCause` to give an explicit context along with the cause.

```scala
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._
import scala.util.Try

// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

def add(a: String, b: String): Either[Throwable, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(Error.withMessageAndCause(NonEmptyString("Error During Addition Operation"), e)),
        i => Right(i)
    )

add("1", "a")
// res8: Either[Throwable, Int] = Left(
//   value = ErrorImpl(
//     primaryErrorMessage = Error During Addition Operation,
//     secondaryErrorMessages = Vector(),
//     causes = Vector(repl.MdocSession$App7$InvalidIntException)
//   )
// )
```

The observant reader probably also noticed that both `Error.fromThrowable` and `Error.withMessageAndCause` inserted the underlying `InvalidIntException` into the `causes` `Vector`. `causes` is similar to `getCause` on `Throwable` except that it allows the modeling of more than one cause. When you invoke `getCause` on `Error` you will get either the first error in the `Vector` or `null` (to comply with the `Throwable` API).

When working with a domain specific error you extend `Error` just as you might extend `RuntimeException`.

```scala
sealed trait OpError extends Error
object OpError {
    case class ParseError(context: String) extends OpError {
        override val primaryErrorMessage: NonEmptyString = NonEmptyString("Error during parsing")
        override val secondaryErrorMessages: Vector[String] = Vector(s"Context: $context")
    }
}
```

Or if your domain is simple enough an out of the box default error with the various functions on the companion object of `Error`, e.g. `withMessage`.

```scala
Error.withMessage(NonEmptyString("Error during parsing"))
// res9: Error = ErrorImpl(
//   primaryErrorMessage = Error during parsing,
//   secondaryErrorMessages = Vector(),
//   causes = Vector()
// )
```

## http4s ##

The http4s module provides utilities for working with http4s related types. These utilities are separate from the ones written for [rfc-7807][rfc-7807].

## HTTP ##

The http module provides a subtype of `Error`, `HttpError`. This type implements the structure defined in [rfc-7807][rfc-7807]. Strictly speaking, `HttpError` is a bit _more restrictive_ than [RFC 7807][rfc-7807] requires. For a truly accurate mapping you can use the related `HttpProblem` type, but this is discouraged for anything other than parsing. Both types have a trivial implementation included in their companion objects. If you don't need extension keys in your [RFC 7807][rfc-7807] JSON, then these types are perfectly fine to use directly.

This module does not specify an particular HTTP library and thus should be able to be integrated into any JVM HTTP library, nor does it specify a specific serialization library or format. As such, it is not very useful on its own. You'll probably want to look at the `http-circe` or `http4s-circe` modules. (PRs are welcome to add support for other JSON/XML libraries).

## HTTP Circe ##

This module adds serialization support for the `HttpError` and `HttpProblem` types via [Circe][circe] codecs for both of these traits as well as their trivial implementations. It also provides two types `ExtensibleCirceHttpError` and `ExtensibleCirceHttpProblem` which are the same as `HttpError` and `HttpProblem` except that they also include a reference to their own JSON representation. Extending these traits allows for adding [RFC 7807][rfc-7807] extension keys. That being said, if you don't need extension keys using the more simple `HttpError` type is recommended.

Users of this library can use the `SimpleCirceHttpError` or `SimpleCirceHttpProblem` types directly, or mix in `CirceHttpError` or `CirceHttpProblem` into their own error ADTs.

[circe]: https://github.com/circe/circe "Circe"

## http4s Circe ##

The http4s circe module provides middlewares which operate on `HttpError` or `HttpProblem` values.

### CirceHttpErrorToResponse ####

`CirceHttpErrorToResponse` is a server middleware which automatically transforms any `HttpError` or `HttpProblem` types into the appropriate response structures as defined in [rfc-7807][rfc-7807].

### PassthroughCirceHttpError ###

`PassthroughCirceHttpError` is a client middleware which checks for `application/problem+json` responses, and if it finds one decodes it and re-raises it in the current `F` context. You can combine this with `CirceHttpErrorToResponse` in order to have a http service passthrough errors. _Caution_, you should only do this if trust the downstream service which may be generating the `application/problem+json`.

[rfc-7807]: https://tools.ietf.org/html/rfc7807 "RFC 7807"

[refined]: https://github.com/fthomas/refined "Refined"
