package org.app.gs1parser.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Gs1Ai(
    val id: String,
    val length: Int,
    val description: String,
    val dataTitle: String,
    @SerialName("fnc1") val fnc1Required: Boolean,
    val decimals: Int,
    val shortName: String,
    val dataType: String,
)
