package dev.banking.asyncapi.generator.core.generator.java.model

/**
 * Template context for a Java class.
 *
 * @property packageName the package of the class
 * @property className the class name
 * @property classDocLines the documentation lines of the class
 * @property fields the template fields of each property, in contract order
 * @property allFields the template fields of every property, for the all-arguments constructor
 * @property hasFields whether the class has properties
 * @property imports the sorted imports
 * @property implementsClause the `implements` clause, or empty
 * @property classAnnotations the annotations on the class
 */
internal data class JavaClassTemplate(
    val packageName: String,
    val className: String,
    val classDocLines: List<String>,
    val fields: List<Map<String, Any?>>,
    val allFields: List<Map<String, Any?>> = fields,
    val hasFields: Boolean = fields.isNotEmpty(),
    val imports: List<String> = emptyList(),
    val implementsClause: String = "",
    val classAnnotations: List<String> = emptyList(),
)
