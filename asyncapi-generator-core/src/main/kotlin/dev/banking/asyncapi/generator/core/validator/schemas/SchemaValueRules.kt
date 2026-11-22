package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_CONST_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEFAULT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_UNIQUE

/**
 * Validates the keywords that declare which values a schema accepts: `type`, `enum`, `const` and `default`.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaValueRules(
    private val asyncApiContext: AsyncApiContext,
) {
    /** Reports a `type` that is not a valid type name or a list of unique valid type names. */
    fun checkType(schema: CheckedSchema) {
        when (val type = schema.node.type) {
            null -> checkMissingType(schema)
            is String -> checkTypeName(schema, type)
            is List<*> -> checkTypeArray(schema, type)
            else ->
                typeError(
                    schema,
                    SCHEMA_TYPE,
                    "${schema.path.child("Schema")} 'type' field must be a string or an array of strings. " +
                        "Found: ${type::class.simpleName}",
                )
        }
    }

    /** Reports an `enum` that is empty or lists a value twice. */
    fun checkEnum(schema: CheckedSchema) {
        val node = schema.node
        val enum = node.enum ?: return
        val location = asyncApiContext.modelTracking.getSourceLocation(node, node::enum)
        if (enum.isEmpty()) {
            schema.results.error(
                SCHEMA_ENUM_NONEMPTY,
                "${schema.path} 'enum' must be a non-empty array of unique values.",
                sourceLocation = location,
            )
        }
        if (SchemaValueSemantics.hasDuplicates(enum)) {
            schema.results.error(
                SCHEMA_ENUM_UNIQUE,
                "${schema.path} 'enum' contains duplicate values.",
                sourceLocation = location,
            )
        }
    }

    /** Reports a `const` whose value does not match the declared `type`. */
    fun checkConst(schema: CheckedSchema) {
        val node = schema.node
        val type = node.type
        val hasConst = node.constSet || node.const != null
        if (type != null && hasConst && !SchemaValueSemantics.isCompatible(node.const, type)) {
            schema.results.error(
                SCHEMA_CONST_TYPE,
                "${schema.path} 'const' value '${node.const}' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::const),
            )
        }
    }

    /** Reports a `default` whose value does not match the declared `type`. */
    fun checkDefault(schema: CheckedSchema) {
        val node = schema.node
        val type = node.type
        val hasDefault = node.defaultSet || node.default != null
        if (type != null && hasDefault && !SchemaValueSemantics.isCompatible(node.default, type)) {
            schema.results.error(
                SCHEMA_DEFAULT_TYPE,
                "${schema.path} default value '${node.default}' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::default),
            )
        }
    }

    private fun checkMissingType(schema: CheckedSchema) {
        val node = schema.node
        if ("type" in asyncApiContext.modelTracking.getFieldNames(node)) {
            schema.results.error(
                SCHEMA_TYPE,
                "${schema.path} Schema 'type' field must be a string or an array of strings. Found: null",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, "type"),
            )
        }
    }

    private fun checkTypeName(
        schema: CheckedSchema,
        type: String,
    ) {
        if (type !in ALLOWED_TYPES) {
            typeError(
                schema,
                SCHEMA_TYPE,
                "${schema.path.child("Schema")} type '$type' is not valid. Must be one of: " +
                    ALLOWED_TYPES.joinToString(),
            )
        }
    }

    private fun checkTypeArray(
        schema: CheckedSchema,
        type: List<*>,
    ) {
        val typePath = schema.path.child("Schema")
        if (type.isEmpty()) {
            typeError(schema, SCHEMA_TYPE_ARRAY_NONEMPTY, "$typePath 'type' array must contain at least one type.")
        }
        val typeList = type.filterIsInstance<String>()
        if (typeList.size != type.size) {
            typeError(
                schema,
                SCHEMA_TYPE,
                "$typePath all elements in 'type' array must be strings. Found non-string elements.",
            )
        }
        val invalidTypes = typeList.filter { it !in ALLOWED_TYPES }
        if (invalidTypes.isNotEmpty()) {
            typeError(
                schema,
                SCHEMA_TYPE,
                "$typePath types ${invalidTypes.joinToString()} are not valid. Must be one " +
                    "of: ${ALLOWED_TYPES.joinToString()}",
            )
        }
        if (typeList.distinct().size != typeList.size) {
            typeError(schema, SCHEMA_TYPE_ARRAY_UNIQUE, "$typePath 'type' array must contain unique values.")
        }
    }

    private fun typeError(
        schema: CheckedSchema,
        rule: ValidationRule,
        message: String,
    ) {
        schema.results.error(
            rule,
            message,
            sourceLocation = asyncApiContext.modelTracking.getSourceLocation(schema.node, schema.node::type),
        )
    }

    private companion object {
        val ALLOWED_TYPES = setOf("string", "number", "integer", "boolean", "array", "object", "null")
    }
}
