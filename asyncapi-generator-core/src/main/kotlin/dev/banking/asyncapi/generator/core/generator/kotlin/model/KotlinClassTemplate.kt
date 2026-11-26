package dev.banking.asyncapi.generator.core.generator.kotlin.model

/**
 * Template context for a Kotlin data class.
 *
 * @property packageName the package of the class
 * @property className the class name
 * @property classDocLines the documentation lines of the class
 * @property fields the template fields of each property, in contract order
 * @property hasFields whether the class has properties
 * @property imports the sorted imports
 * @property implementsClause the supertype clause, or empty
 * @property classAnnotations the annotations on the class
 */
internal data class KotlinClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<Map<String, Any?>>,
    val hasFields: Boolean = fields.isNotEmpty(),
    val imports: List<String> = emptyList(),
    val implementsClause: String = "",
    val classAnnotations: List<String> = emptyList(),
)
