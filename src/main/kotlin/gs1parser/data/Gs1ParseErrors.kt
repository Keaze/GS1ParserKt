package org.app.gs1parser.data

sealed class Gs1ParseErrors {
    data class AiNotFound(val barcode: String) : Gs1ParseErrors()
    object NotAGs1Barcode: Gs1ParseErrors()
    data class ValueLengthError(val aiId: String, val aiLength: Int, val barcode: String): Gs1ParseErrors()
    data class GsNotFound(val aiId: String, val barcode: String) : Gs1ParseErrors()
}
