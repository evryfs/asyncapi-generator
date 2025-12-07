package com.tietoevry.banking.asyncapi.generator.core.generator.java.model

data class JavaFieldTemplate(
    val name: String,
    val type: String,
    val getterName: String,
    val setterName: String,
    val docFirstLine: String?,
    val docTailLines: List<String> = emptyList(),
    val last: Boolean = false,
    val annotations: List<String> = emptyList()
)
