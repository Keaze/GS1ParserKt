package org.app.gs1parser.data

import org.app.gs1parser.GS1Barcode
import org.app.utils.ResultKt

sealed class Gs1ParseErrors {
    object AiNotFound: Gs1ParseErrors()
    object NotAGs1Barcode: Gs1ParseErrors()
    data class ValueLengthError(val aiId: String, val aiLength: Int, val barcode: String): Gs1ParseErrors()
}
