package com.codescene.jetbrains.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeSmell( //ok
    val category: String, //ok
    @SerialName("highlight-range") val highlightRange: HighlightRange, //ok
    val details: String //ok
)

@Serializable
data class HighlightRange( //range
    @SerialName("start-line") val startLine: Int, //ok
    @SerialName("start-column") val startColumn: Int,  //ok
    @SerialName("end-line") val endLine: Int,  //ok
    @SerialName("end-column") val endColumn: Int  //ok
)