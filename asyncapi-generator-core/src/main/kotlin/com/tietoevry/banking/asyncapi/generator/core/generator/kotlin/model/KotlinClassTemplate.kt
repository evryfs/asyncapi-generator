package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

data class KotlinClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<KotlinFieldTemplate>,
    val imports: List<String> = emptyList(),
    val implementsClause: String = "",
)
