package com.tietoevry.banking.asyncapi.generator.core.generator.java.model

data class JavaClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<JavaFieldTemplate>,
    val allFields: List<JavaFieldTemplate> = fields,
    val imports: List<String> = emptyList(),
    val implementsClause: String = ""
)
