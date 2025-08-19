package org.app.gs1parser.data

import org.app.gs1parser.GS1Barcode

data class Gs1Success(val barcode: GS1Barcode, val barcodeValues: List<AiValue> = listOf())
