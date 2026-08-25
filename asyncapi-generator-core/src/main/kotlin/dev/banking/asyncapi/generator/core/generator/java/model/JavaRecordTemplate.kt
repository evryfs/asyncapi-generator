package dev.banking.asyncapi.generator.core.generator.java.model

/**
 * Template context for Java record source generation.
 */
data class JavaRecordTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<Map<String, Any?>>,
    val hasFields: Boolean = fields.isNotEmpty(),
    val imports: List<String> = emptyList(),
    val implementsClause: String = "",
    val classAnnotations: List<String> = emptyList(),
)
