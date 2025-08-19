package result

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.app.utils.ResultKt
import java.util.Optional
import kotlin.math.sqrt

class ResultTest : ShouldSpec({

    context("fromOptional") {
        should("return Success when optional is present") {
            val opt = Optional.of(5)
            val actual: ResultKt<Int, String> = ResultKt.fromOptional(opt, "The Optional is empty.")
            actual shouldBe ResultKt.success(5)
        }
        should("return Failure when optional is empty (custom message)") {
            val opt = Optional.empty<Int>()
            val actual = ResultKt.fromOptional(opt, "The Optional is empty.")
            actual shouldBe ResultKt.failure("The Optional is empty.")
        }
        should("return Failure when optional is null") {
            val opt: Optional<Int>? = null
            val actual = ResultKt.fromOptional(opt, "ignored")
            actual shouldBe ResultKt.failure("Optional is null")
        }
        should("return Failure with default 'Empty' when optional is empty and no message provided") {
            val opt = Optional.empty<Int>()
            val actual = ResultKt.fromOptional(opt)
            actual shouldBe ResultKt.failure("Empty")
        }
    }

    context("sequence and sequencing variants") {
        should("collect all success values") {
            val list = listOf(
                ResultKt.success(1),
                ResultKt.success(2),
                ResultKt.success<Int, String>(3)
            )
            val seq = ResultKt.sequence(list)
            seq.shouldBe(ResultKt.Companion.success(listOf(1, 2, 3)))
            // orThrow() should yield the list
            (seq as ResultKt.Success).value shouldBe listOf(1, 2, 3)
        }
        should("collect errors into a list when any fail") {
            val list: List<ResultKt<Int, String>> = listOf(
                ResultKt.success(1),
                ResultKt.failure("Error"),
                ResultKt.success(3)
            )
            val seq = ResultKt.sequence(list)
            // access error directly
            (seq as ResultKt.Failure).error shouldBe listOf("Error")
            // orThrow() should throw IllegalStateException("Not successful")
            val ex = shouldThrow<IllegalStateException> { seq.orThrow() }
            ex.message shouldBe "Not successful"
        }
        should("reduce errors to single error with error mapper") {
            val list = listOf(
                ResultKt.success<Int, String>(1),
                ResultKt.failure("Error"),
                ResultKt.failure("Error2")
            )
            val seq = ResultKt.sequenceWithErrorMapper(list) { a, b -> a + b }
            (seq as ResultKt.Failure).error shouldBe "ErrorError2"
        }
        should("use constant custom error on any failure") {
            val list = listOf(
                ResultKt.success<Int, String>(1),
                ResultKt.failure("Error"),
                ResultKt.failure("Error2")
            )
            val seq = ResultKt.sequence(list, "TestError")
            (seq as ResultKt.Failure).error shouldBe "TestError"
        }
    }

    context("getOrElse / getOrElseGet") {
        should("return success value from getOrElse when Success") {
            val resultKt: ResultKt<String, String> = ResultKt.Companion.success("Success ResultKt")
             resultKt.getOrElse("No ResultKt") shouldBe "Success ResultKt"
        }
        should("return default value from getOrElse when Failure") {
            val resultKt: ResultKt<String, String> = ResultKt.Companion.failure("Error ResultKt")
             resultKt.getOrElse("Default Value") shouldBe "Default Value"
        }
        should("getOrElseGet returns success value if Success") {
            val expected = 10
            val resultKt: ResultKt<Int, String> = ResultKt.Companion.success(expected)
             resultKt.getOrElseGet { 20 } shouldBe expected
        }
        should("getOrElseGet returns default value if Failure") {
            val expected = 20
            val resultKt: ResultKt<Int, String> = ResultKt.Companion.failure("Some error")
             resultKt.getOrElseGet { expected } shouldBe expected
        }
    }

    context("or chaining") {
        should("return first when both are Success") {
            val r1: ResultKt<String, String> = ResultKt.Companion.success("Success1")
            val r2: ResultKt<String, String> = ResultKt.Companion.success("Success2")
            r1.or(r2) shouldBe r1
        }
        should("return first when first is Success and second is Failure") {
            val r1: ResultKt<String, String> = ResultKt.success("Success")
            val r2: ResultKt<String, String> = ResultKt.failure("Failure")
            r1.or(r2) shouldBe r1
        }
        should("return second when first is Failure and second is Success") {
            val r1: ResultKt<String, String> = ResultKt.failure("Failure")
            val r2: ResultKt<String, String> = ResultKt.success("Success")
            r1.or(r2) shouldBe r2
        }
        should("return second when both are Failure") {
            val r1: ResultKt<String, String> = ResultKt.failure("Failure1")
            val r2: ResultKt<String, String> = ResultKt.failure("Failure2")
            r1.or(r2) shouldBe r2
        }
        should("not invoke supplier when current is Success") {
            val r1: ResultKt<String, String> = ResultKt.success("Successful ResultKt")
            var called = 0
            val res = r1.or {
                called++
                 ResultKt.success("Other")
            }
            res.orThrow() shouldBe "Successful ResultKt"
            called shouldBe 0
        }
        should("invoke supplier when current is Failure") {
            val r1: ResultKt<String, String> = ResultKt.failure("Failure ResultKt")
            var called = 0
            val res = r1.or {
                called++
                 ResultKt.success("Other ResultKt")
            }
            res.orThrow() shouldBe "Other ResultKt"
            called shouldBe 1
        }
    }

    context("orThrow() and error()") {
        should("orThrow() throws when Failure") {
            val r: ResultKt<Int, String> = ResultKt.failure("Failure")
            shouldThrow<IllegalStateException> { r.orThrow() }
        }
        should("orThrow() returns value when Success") {
            val r: ResultKt<Int, String> = ResultKt.success(5)
            r.orThrow() shouldBe 5
        }
        should("error returns error for Failure and throws for Success") {
            val failure: ResultKt<Int, String> = ResultKt.failure("An Error")
            failure.error() shouldBe "An Error"
            val success: ResultKt<Int, String> = ResultKt.success(10)
            val ex = shouldThrow<IllegalStateException> { success.error() }
            ex.message shouldBe "No Error"
        }
    }

    context("map / mapError / flatMap / filter") {
        should("map a Success value") {
            val res: ResultKt<Int, String> = ResultKt.success<Int, String>(5).map { it * 2 }
            res.isSuccessful.shouldBeTrue()
            (res as ResultKt.Success).value shouldBe 10
        }
        should("map keeps Failure unchanged") {
            val res: ResultKt<Int, String> = ResultKt.failure<Int, String>("Failure").map { it * 2 }
            res shouldBe ResultKt.failure("Failure")
        }
        should("mapError applies only to Failure") {
            val failureMapped = ResultKt.failure<String, String>("Unsuccessful").mapError { "Mapped: $it" }
            (failureMapped as ResultKt.Failure).error shouldBe "Mapped: Unsuccessful"
            val successMapped = ResultKt.success<String, String>("Successful").mapError { "Mapped: $it" }
            (successMapped as ResultKt.Success).value shouldBe "Successful"
        }
        should("flatMap chains on Success and propagates Failure") {
            val a: ResultKt<Int, String> = ResultKt.success(2)
            val b: ResultKt<Int, String> = a.flatMap { ResultKt.success(it * 2) }
            b.isSuccessful.shouldBeTrue()
            (b as ResultKt.Success).value shouldBe 4

            val f: ResultKt<Int, String> = ResultKt.failure("Failure")
            val b2: ResultKt<Int, String> = f.flatMap { ResultKt.success(it * 2) }
            b2.isSuccessful.shouldBeFalse()
            (b2 as ResultKt.Failure).error shouldBe "Failure"
        }
        should("filter converts to Failure when predicate fails and keeps Success otherwise") {
            val r: ResultKt<Int, String> = ResultKt.success(10)
            val fail: ResultKt<Int, String> = r.filter({ it > 10 }, "Not greater than 10")
            fail.isSuccessful.shouldBeFalse()
            (fail as ResultKt.Failure).error shouldBe "Not greater than 10"

            val ok: ResultKt<Int, String> = r.filter({ it < 11 }, "Not less than 10")
            ok.isSuccessful.shouldBeTrue()
            (ok as ResultKt.Success).value shouldBe 10
        }
    }

    context("ifSuccessful / ifSuccessfulOrElse") {
        should("execute consumer only on Success") {
            val sb = StringBuilder()
             ResultKt.success<String, String>("Success").ifSuccessful { sb.append(it) }
            sb.toString() shouldBe "Success"

            val sb2 = StringBuilder()
             ResultKt.failure<String, String>("Failure").ifSuccessful { sb2.append(it) }
            sb2.toString() shouldBe ""
        }
        should("run proper branch in ifSuccessfulOrElse") {
            var executed = false
             ResultKt.success<String, String>("Success").ifSuccessfulOrElse({ executed = true }, { executed = true })
            executed.shouldBeTrue()

            executed = false
             ResultKt.failure<String, String>("Failure").ifSuccessfulOrElse({ executed = true }, { executed = true })
            executed.shouldBeTrue()
        }
    }

    context("map2") {
        should("combine two successes (list error variant)") {
            val a: ResultKt<Int, String> = ResultKt.success(2)
            val b: ResultKt<Int, String> = ResultKt.success(3)
            val r: ResultKt<Int, List<String>> = a.map2(b) { x, y -> x + y }
            r.isSuccessful.shouldBeTrue()
            (r as ResultKt.Success).value shouldBe 5
        }
        should("return failure list when one fails") {
            val a: ResultKt<Int, String> = ResultKt.success(2)
            val b: ResultKt<Int, String> = ResultKt.failure("An error occurred")
            val r: ResultKt<Int, List<String>> = a.map2(b) { x, y -> x + y }
            r.isSuccessful.shouldBeFalse()
            (r as ResultKt.Failure).error shouldContainExactly listOf("An error occurred")
        }
        should("combine failures using error mapper into single error") {
            val a: ResultKt<Int, String> = ResultKt.failure("Err1")
            val b: ResultKt<Int, String> = ResultKt.failure("Err2")
            val r: ResultKt<Int, String> = a.map2(b, { _, _ -> 0 }) { e1, e2 -> "$e1+$e2" }
            r.isSuccessful.shouldBeFalse()
            (r as ResultKt.Failure).error shouldBe "Err1+Err2"
        }
        should("various map2 scenarios as in original tests") {
            val a: ResultKt<Int, String> = ResultKt.ofNullable(2, "Null Value")
            val successful: ResultKt<String, List<String>> = a.map2( ResultKt.success(5)) { x, y -> "$x${y} processed" }
            successful.isSuccessful.shouldBeTrue()
            (successful as ResultKt.Success).value shouldBe "25 processed"

            val b: ResultKt<Int, String> = ResultKt.failure("Failure Error")
            val failure1: ResultKt<String, List<String>> = a.map2(b) { x, y -> "$x${y} processed" }
            failure1.isSuccessful.shouldBeFalse()
            (failure1 as ResultKt.Failure).error shouldContainExactly listOf("Failure Error")

            val failure2: ResultKt<String, List<String>> = b.map2(a) { x, y -> "$x${y} processed" }
            failure2.isSuccessful.shouldBeFalse()
            (failure2 as ResultKt.Failure).error shouldContainExactly listOf("Failure Error")

            val error: ResultKt<String, String> = ResultKt.failure("Error")
            val two: ResultKt<String, List<String>> = error.map2(
                 ResultKt.failure("Failure Error")
            ) { x: String, y: String -> "$x${y} processed" }
            two.isSuccessful.shouldBeFalse()
            (two as ResultKt.Failure).error shouldContainExactly listOf("Error", "Failure Error")
        }
    }

    context("recover and recoverWith") {
        should("recover a success as identity and failure via mapper") {
            val s: ResultKt<Int, String> = ResultKt.success(1)
            val r1: ResultKt<Int, String> = s.recover { 0 }
            r1.isSuccessful.shouldBeTrue()
            r1.getOrElse(-1) shouldBe 1

            val f: ResultKt<Int, String> = ResultKt.failure("Error message")
            val r2: ResultKt<Int, String> = f.recover { 0 }
            r2.isSuccessful.shouldBeTrue()
            r2.getOrElse(-1) shouldBe 0
        }
        should("recoverWith to success or failure and be no-op for success") {
            val initial: ResultKt<Double, String> = ResultKt.failure("initial error")
            val recSuccess: ResultKt<Double, String> = initial.recoverWith { ResultKt.success(sqrt(4.0)) }
            recSuccess.isSuccessful.shouldBeTrue()
            recSuccess.orThrow() shouldBe 2.0

            val recFailure: ResultKt<Double, String> = initial.recoverWith { ResultKt.failure("error persisted") }
            recFailure.isSuccessful.shouldBeFalse()
            (recFailure as ResultKt.Failure).error shouldBe "error persisted"

            val already: ResultKt<Double, String> = ResultKt.success(3.0)
            val notCalled: ResultKt<Double, String> = already.recoverWith { ResultKt.success(sqrt(4.0)) }
            notCalled.isSuccessful.shouldBeTrue()
            notCalled.orThrow() shouldBe 3.0
        }
    }

    context("ofNullable") {
        should("return Success when value not null") {
            val value = "Test Value"
            val res: ResultKt<String, String> = ResultKt.ofNullable(value)
            res.isSuccessful.shouldBeTrue()
            (res as ResultKt.Success).value shouldBe value
        }
        should("return Failure with provided message when value is null") {
            val res: ResultKt<String, String> = ResultKt.ofNullable<String, String>(null, "An error occurred")
            res.isSuccessful.shouldBeFalse()
            (res as ResultKt.Failure).error shouldBe "An error occurred"
        }
        should("return Failure with default message 'Object is Null' when value is null and no message provided") {
            val res: ResultKt<String, String> = ResultKt.ofNullable<String>(null)
            res.isSuccessful.shouldBeFalse()
            (res as ResultKt.Failure).error shouldBe "Object is Null"
        }
    }
})
