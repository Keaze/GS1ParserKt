import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.app.gs1parser.GS1Scanner
import org.app.gs1parser.data.AiValue
import org.app.gs1parser.data.Gs1ParseErrors
import org.app.gs1parser.data.Gs1Success
import org.app.utils.ResultKt

class GS1ScannerTest : ShouldSpec({
    val fnc1 = "]C1"
    val gs = "<GS>"
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
        should("return AiNotFound on invalid ai") {
            val barcode = fnc1 + "XXXXX"
            scanner.parse(barcode) shouldBe ResultKt.failure(Gs1ParseErrors.AiNotFound(barcode.substring(fnc1.length)))
        }
        should("return NotAGs1Barcode on empty string") {
            scanner.parse("") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        should("return NotAGs1Barcode missing fnc1") {
            scanner.parse("TEST") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        should("return NotAGs1Barcode on empty barcode after fnc1") {
            scanner.parse(fnc1 + "") shouldBe ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        should("return AiNotFound when value longer than AI length") {
            val barcode = fnc1 + "111234599" // AI 01 requires 14 digits
            scanner.parse(barcode) shouldBe ResultKt.failure(Gs1ParseErrors.AiNotFound("9"))
        }
        should("barcode with invalid var ai in the middle should return error") {
            val barcode = fnc1 + "101234567890012345678901234${gs}0112345678901234"
            scanner.parse(barcode) shouldBe ResultKt.failure(
                Gs1ParseErrors.GsNotFound(
                    "10",
                    "101234567890012345678901234${gs}0112345678901234"
                )
            )
        }
    }

    context("parse simple GS1 Barcode") {

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


        should("barcode with valid var ai at the end should have the correct value") {
            val barcode = fnc1 + "1012345"
            val result = scanner.parse(barcode)
            val barcodeValues = getBarcodeValues(result)
            barcodeValues.size shouldBe 1
            barcodeValues.first().ai.id shouldBe "10"
            barcodeValues.first().value shouldBe "12345"
        }
        should("barcode with valid var ai and max length in the middle should have the correct value") {
            val barcode = fnc1 + "1012345678900123456789${gs}0112345678901234"
            val result = scanner.parse(barcode)
            val barcodeValues = getSuccess(result)
            val batch = barcodeValues.getById("10")
            val gtin = barcodeValues.getById("01")
            batch.shouldNotBeNull()
            gtin.shouldNotBeNull()

            batch.value shouldBe "12345678900123456789"
            gtin.value shouldBe "12345678901234"
        }
        should("barcode with valid var ai in the middle should have the correct value") {
            val barcode = fnc1 + "101234567890012345678${gs}0112345678901234"
            val result = scanner.parse(barcode)
            val barcodeValues = getSuccess(result)
            val batch = barcodeValues.getById("10")
            val gtin = barcodeValues.getById("01")
            batch.shouldNotBeNull()
            gtin.shouldNotBeNull()

            batch.value shouldBe "1234567890012345678"
            gtin.value shouldBe "12345678901234"
        }

    }
    context("Parse complex barcodes") {

        should("sucessful parse variable ais") {
            val gs1 = "${fnc1}10345678${gs}213456789012"
            val result = scanner.parse(gs1)
            val barcodeValues = getSuccess(result)
            val batch = barcodeValues.getById("10")
            val valueTwo = barcodeValues.getById("21")
            batch.shouldNotBeNull()
            valueTwo.shouldNotBeNull()
            batch.value shouldBe "345678"
            valueTwo.value shouldBe "3456789012"
        }
        should("sucessful parse multiple ais with one decimal value") {
            val gs1 = "${fnc1}019909999954321031030011251522052110Abc123"
            val result = scanner.parse(gs1)
            val barcodeValues = getSuccess(result)
            val valueOne = barcodeValues.getById("01")
            val valueTwo = barcodeValues.getById("10")
            val valueThree = barcodeValues.getById("15")
            val valueFour = barcodeValues.getById("3103")
            valueOne.shouldNotBeNull()
            valueTwo.shouldNotBeNull()
            valueThree.shouldNotBeNull()
            valueFour.shouldNotBeNull()
            valueOne.value shouldBe "99099999543210"
            valueTwo.value shouldBe "Abc123"
            valueThree.value shouldBe "220521"
            valueFour.value shouldBe "001.125"
        }

    }


})

private fun getBarcodeValues(result: ResultKt<Gs1Success, Gs1ParseErrors>): List<AiValue> {
    getSuccess(result).barcodeValues.let { barcodeValues -> return barcodeValues }
}

private fun getSuccess(result: ResultKt<Gs1Success, Gs1ParseErrors>): Gs1Success {
    result.isSuccessful shouldBe true
    return result.orThrow()
}

