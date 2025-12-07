package com.tietoevry.banking.asyncapi.generator.core.generator.java.model

data class JavaClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<Map<String, Any?>>,
    val allFields: List<Map<String, Any?>> = fields,
    val imports: List<String> = emptyList(),
    val implementsClause: String = ""
)
