import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.app.gs1parser.GS1Scanner
import org.app.gs1parser.data.AiValue
import org.app.gs1parser.data.Gs1ParseErrors
import org.app.gs1parser.data.Gs1Success
import org.app.utils.ResultKt

class GS1ScannerTest : ShouldSpec({
    val fnc1 = "]C1"
    lateinit var scanner: GS1Scanner

    beforeContainer {
        scanner = GS1Scanner.getDefaultParser().orThrow()
    }

    context("isGS1Format") {
        should("return false on empty string") {
            scanner.isGs1Format("") shouldBe false
        }
        should("return false if barcode is only fnc1") {
            scanner.isGs1Format(fnc1) shouldBe false
        }
        should("return true if barcode starts with fnc1 and has more content after") {
            scanner.isGs1Format(fnc1 + "1234214") shouldBe true
        }
    }

    context("parse invalid barcode") {
        should("return NotAGs1Barcode on empty string") {
            scanner.parse("") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        should("return NotAGs1Barcode missing fnc1") {
            scanner.parse("TEST") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        should("return NotAGs1Barcode on empty barcode after fnc1") {
            scanner.parse(fnc1 + "") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
    }

    context("parse simple GS1 Barcode") {
        should("return AiNotFound on invalid ai") {
            val barcode = fnc1 + "XXXXX"
            scanner.parse(barcode) shouldBe ResultKt.failure(Gs1ParseErrors.AiNotFound)
        }

        should("barcode with valid static ai should have the correct value") {
            val barcode = fnc1 + "0112345678901234"
            val result = scanner.parse(barcode)
            val barcodeValues = getBarcodeValues(result)
            barcodeValues.size shouldBe 1
            barcodeValues.first().ai.id shouldBe "01"
            barcodeValues.first().value shouldBe "12345678901234"
        }

        should("return ValueLengthError when value shorter than AI length") {
            val barcode = fnc1 + "011234" // AI 01 requires 14 digits
            scanner.parse(barcode) shouldBe ResultKt.failure(
                Gs1ParseErrors.ValueLengthError("01", 14, "011234")
            )
        }
    }


})

private fun getBarcodeValues(result: ResultKt<Gs1Success, Gs1ParseErrors>): List<AiValue> {
    result.isSuccessful shouldBe true
    val barcodeValues = result.orThrow().barcodeValues
    return barcodeValues
}

