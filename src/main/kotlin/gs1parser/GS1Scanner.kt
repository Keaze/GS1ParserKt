package org.app.gs1parser

import org.app.gs1parser.data.AiValue
import org.app.gs1parser.data.Gs1Ai
import org.app.gs1parser.data.Gs1ParseErrors
import org.app.gs1parser.data.Gs1Success
import org.app.utils.JsonResourceReader
import org.app.utils.ResultKt

const val GS1_AI_FILE = "gs1.json"

private const val DEFAULT_FNC1 = "]C1"
private const val DEFAULT_GS = "<GS>"

class GS1Scanner(val fnc1: String = DEFAULT_FNC1, val gs: String = "|", val aiList: List<Gs1Ai> = emptyList()) {
    fun isGs1Format(barcode: String): Boolean {
        if (barcode.isBlank()) {
            return false
        }
        return barcode.startsWith(fnc1) and (barcode.length > fnc1.length)
    }

    fun parse(barcode: String): ResultKt<Gs1Success, Gs1ParseErrors> {
        if (!isGs1Format(barcode)) {
            return ResultKt.failure(Gs1ParseErrors.NotAGs1Barcode)
        }
        val result= Gs1Success(GS1Barcode(barcode))
        return startParse(barcode.substring(fnc1.length), result)
    }

    private tailrec fun startParse(barcode: String, parseResult: Gs1Success): ResultKt<Gs1Success, Gs1ParseErrors> {
        val ai = findAi(barcode).flatMap { parseAi(it, barcode) }
        val newResult = ai.map { parseResult.copy(barcodeValues = parseResult.barcodeValues + it) }
        if (newResult.isFailure) {
            return newResult
        }
        val nextBarcode = getRemainingBarcode(ai.orThrow(), barcode)
        if (nextBarcode.isBlank()) {
            return newResult
        }
        return startParse(nextBarcode, parseResult)
    }

    private fun findAi(barcode: String): ResultKt<Gs1Ai, Gs1ParseErrors> {
        return aiList.firstOrNull { barcode.startsWith(it.id) }?.let {
            ResultKt.success(it)
        } ?: ResultKt.failure(Gs1ParseErrors.AiNotFound)
    }
    private fun parseAi(ai: Gs1Ai, barcode: String): ResultKt<AiValue, Gs1ParseErrors>{
        return if(ai.fnc1Required){
            TODO()
        } else {
            parseStaticAi(ai, barcode)
        }

    }

    private fun getRemainingBarcode(aiValue: AiValue, barcode: String): String {
        return barcode.substring(aiValue.ai.id.length + aiValue.value.length)
    }

    private fun parseStaticAi(ai: Gs1Ai, barcode: String): ResultKt<AiValue, Gs1ParseErrors> {
        removeAiId(ai.id, barcode).let {
            return (if(it.length < ai.length){
                return ResultKt.failure(Gs1ParseErrors.ValueLengthError(ai.id, ai.length, barcode))
            } else {
                ResultKt.success(AiValue(ai, it))
            })
        }
    }

    private fun removeAiId(id: String, barcode: String): String  = barcode.substring(id.length)


    companion object {
        fun getDefaultParser(fnc1: String = DEFAULT_FNC1, gs: String = DEFAULT_GS): ResultKt<GS1Scanner, Exception> {
            val aiList = JsonResourceReader.readJsonFromResource<List<Gs1Ai>>(GS1_AI_FILE)
            return aiList?.let {
                ResultKt.success(GS1Scanner(fnc1, gs, it))
            } ?: ResultKt.failure(Exception("Unable to load AI list"))
        }
    }
}
data class GS1Barcode (val barcode: String)
fun Gs1ParseErrors.toResult(): ResultKt<Gs1Success, Gs1ParseErrors> = ResultKt.failure(this)
