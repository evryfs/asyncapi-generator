package dev.banking.asyncapi.generator.core.generator.kotlin.model

data class KotlinClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<Map<String, Any?>>,
    val imports: List<String> = emptyList(),
    val implementsClause: String = "",
    val classAnnotations: List<String> = emptyList(),
)
