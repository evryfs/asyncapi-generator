package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

data class KotlinFieldTemplate(
    val name: String,
    val type: String,
    val docFirstLine: String?,
    val docTailLines: List<String> = emptyList(),
    val nullable: Boolean,
    val defaultValue: String? = null,
    val last: Boolean = false,
    val annotations: List<String> = emptyList(),
)
