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
            return Gs1ParseErrors.NotAGs1Barcode.toResult()
        }
        val result= Gs1Success(GS1Barcode(barcode))
        return startParse(barcode.substring(fnc1.length), result)
    }

    private tailrec fun startParse(barcode: String, parseResult: Gs1Success): ResultKt<Gs1Success, Gs1ParseErrors> {
        val ai = findAi(barcode).flatMap { parseAi(it, barcode) }
        val newResult = ai.map { parseResult.copy(barcodeValues = parseResult.barcodeValues + it) }
        lateinit var nextBarcode: String
        when (newResult) {
            is ResultKt.Failure -> return newResult
            is ResultKt.Success -> {
                nextBarcode = getRemainingBarcode(ai.orThrow(), barcode)
                if (nextBarcode.isBlank()) {
                    return newResult
                }
            }
        }
        return startParse(nextBarcode, newResult.orThrow())
    }

    private fun findAi(barcode: String): ResultKt<Gs1Ai, Gs1ParseErrors> {
        return aiList.firstOrNull { barcode.startsWith(it.id) }?.let {
            ResultKt.success(it)
        } ?: Gs1ParseErrors.AiNotFound(barcode).toResult()
    }
    private fun parseAi(ai: Gs1Ai, barcode: String): ResultKt<AiValue, Gs1ParseErrors>{
        return (if (ai.fnc1Required) {
            parseVariableAi(ai, barcode)
        } else {
            parseStaticAi(ai, barcode)
        }).map(this::addDecimalToValue)

    }

    private fun addDecimalToValue(aiValue: AiValue): AiValue {
        return if (aiValue.ai.decimals > 0) {
            aiValue.copy(value = aiValue.value.take(aiValue.ai.decimals) + "." + aiValue.value.substring(aiValue.ai.decimals))
        } else {
            aiValue
        }

    }

    private fun parseVariableAi(ai: Gs1Ai, barcode: String): ResultKt<AiValue, Gs1ParseErrors> {
        removeAiId(ai.id, barcode).let {
            val gsIndex = it.indexOf(gs)
            return if ((gsIndex != -1) and (gsIndex <= ai.length)) {
                it.substring(0, gsIndex).let { value -> ResultKt.success(AiValue(ai, value)) }
            } else {
                if (it.length <= ai.length) {
                    AiValue(ai, it).toResult()
                } else {
                    Gs1ParseErrors.GsNotFound(ai.id, barcode).toResult()
                }
            }
        }
    }

    private fun getRemainingBarcode(aiValue: AiValue, barcode: String): String {
        val valueLength = if (aiValue.ai.decimals <= 0) aiValue.value.length else aiValue.value.length - 1
        val barcodeWithoutAi = barcode.substring(aiValue.ai.id.length + valueLength)
        return if (barcodeWithoutAi.startsWith(gs)) {
            barcodeWithoutAi.substring(gs.length)
        } else {
            barcodeWithoutAi
        }
    }

    private fun parseStaticAi(ai: Gs1Ai, barcode: String): ResultKt<AiValue, Gs1ParseErrors> {
        removeAiId(ai.id, barcode).let {
            return (if(it.length < ai.length){
                Gs1ParseErrors.ValueLengthError(ai.id, ai.length, barcode).toResult()
            } else {
                AiValue(ai, it.substring(0, ai.length)).toResult()
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

fun <V> Gs1ParseErrors.toResult(): ResultKt<V, Gs1ParseErrors> = ResultKt.failure(this)
fun <E> AiValue.toResult(): ResultKt<AiValue, E> = ResultKt.success(this)
