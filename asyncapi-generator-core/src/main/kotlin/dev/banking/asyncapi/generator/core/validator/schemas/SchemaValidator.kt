package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeAny
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class SchemaValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val bindingValidator = BindingValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(schemaName: String, schemaInterface: SchemaInterface, results: ValidationResults) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                validate(schemaInterface.schema, schemaName, results)

            is SchemaInterface.SchemaReference ->
                referenceResolver.resolve(schemaInterface.reference, "Schema", results)

            is SchemaInterface.MultiFormatSchemaInline -> {}
            is SchemaInterface.BooleanSchema -> {}
        }
    }

    fun validate(node: Schema, schemaName: String, results: ValidationResults) {
        validateType(node, schemaName, results)
        validateEnum(node, schemaName, results)
        validateConst(node, schemaName, results)
        validateNumericRange(node, schemaName, results)
        validateStringLength(node, schemaName, results)
        validatePattern(node, schemaName, results)
        validateArray(node, schemaName, results)
        validateObject(node, schemaName, results)
        validateComposition(node, schemaName, results)
        validateDefaultValue(node, schemaName, results)
        validateDiscriminator(node, schemaName, results)
        validateExternalDocs(node, schemaName, results)
        validateBindings(node, schemaName, results)

        // Recursive validation for nested schemas
        node.properties?.forEach { (name, sub) -> validateInterface(name, sub, results) }
        node.definitions?.forEach { (name, sub) -> validateInterface(name, sub, results) }
        node.items?.let { validateInterface(schemaName, it, results) }
        node.additionalItems?.let { validateInterface(schemaName, it, results) }
        node.additionalProperties?.let { validateInterface(schemaName, it, results) }
        node.contains?.let { validateInterface(schemaName, it, results) }
        node.propertyNames?.let { validateInterface(schemaName, it, results) }

        node.allOf?.forEach { sub -> validateInterface(schemaName, sub, results) }
        node.anyOf?.forEach { sub -> validateInterface(schemaName, sub, results) }
        node.oneOf?.forEach { sub -> validateInterface(schemaName, sub, results) }

        node.not?.let { sub -> validateInterface(schemaName, sub, results) }
        node.ifSchema?.let { sub -> validateInterface(schemaName, sub, results) }
        node.thenSchema?.let { sub -> validateInterface(schemaName, sub, results) }
        node.elseSchema?.let { sub -> validateInterface(schemaName, sub, results) }
    }

    private fun validateType(node: Schema, schemaName: String, results: ValidationResults) {
        val type = node.type?.let(::sanitizeAny) ?: return

        val allowedPrimitiveTypes = setOf(
            "string", "number", "integer", "boolean", "array", "object", "null"
        )

        when (type) {
            is String -> {
                if (type.lowercase() !in allowedPrimitiveTypes) {
                    results.error(
                        "Schema '$schemaName' type '$type' is not valid. Must be one of: ${allowedPrimitiveTypes.joinToString()}",
                        asyncApiContext.getLine(node, node::type)
                    )
                }
            }

            is List<*> -> {
                // Trim quotes and lowercase each element in the list before validation
                val typeList = type.mapNotNull { item -> (item?.let(::sanitizeAny) as String).lowercase() }

                if (typeList.size != type.size) {
                    results.error(
                        "Schema '$schemaName' all elements in 'type' array must be strings. Found non-string elements.",
                        asyncApiContext.getLine(node, node::type)
                    )
                    return
                }

                val invalidTypes = typeList.filter { it !in allowedPrimitiveTypes }
                if (invalidTypes.isNotEmpty()) {
                    results.error(
                        "Schema '$schemaName' types ${invalidTypes.joinToString()} are not valid. Must be one of: ${allowedPrimitiveTypes.joinToString()}",
                        asyncApiContext.getLine(node, node::type)
                    )
                }

                val nullCount = typeList.count { it == "null" }
                if (nullCount > 1) {
                    results.error(
                        "Schema '$schemaName''type' array must contain 'null' at most once.",
                        asyncApiContext.getLine(node, node::type)
                    )
                }

                val nonNullTypes = typeList.filter { it != "null" }
                if (nonNullTypes.size > 1) {
                    results.error(
                        "Schema '$schemaName' - If 'type' is an array, it should contain at most one non-'null' type. Found: ${nonNullTypes.joinToString()}",
                        asyncApiContext.getLine(node, node::type)
                    )
                }
            }

            else -> {
                results.error(
                    "Schema '$schemaName' 'type' field must be a string or an array of strings. Found: ${type::class.simpleName}",
                    asyncApiContext.getLine(node, node::type)
                )
            }
        }
    }


    private fun validateEnum(node: Schema, schemaName: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum?.let(::sanitizeAny) } ?: return
        if (enum.isEmpty()) {
            results.warn(
                "Schema '$schemaName' 'enum' is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::enum),
            )
        }
    }

    private fun validateConst(node: Schema, schemaName: String, results: ValidationResults) {
        val const = node.const?.let(::sanitizeAny) ?: return
        val type = node.type?.let(::sanitizeAny) ?: return
        if (!isDefaultCompatible(const, type)) {
            results.error(
                "Schema '$schemaName' 'const' value '$const' does not match declared type '$type'.",
                asyncApiContext.getLine(node, node::const),
            )
        }
    }

    private fun validateNumericRange(node: Schema, schemaName: String, results: ValidationResults) {
        val minimum = node.minimum?.toDouble()
        val maximum = node.maximum?.toDouble()
        val exclusiveMinimum = node.exclusiveMinimum?.toDouble()
        val exclusiveMaximum = node.exclusiveMaximum?.toDouble()

        if (minimum != null && maximum != null && minimum > maximum) {
            results.error(
                "Schema '$schemaName' 'minimum' ($minimum) cannot be greater than 'maximum' ($maximum).",
                asyncApiContext.getLine(node, node::minimum)
            )
        }
        if (exclusiveMinimum != null && exclusiveMaximum != null && exclusiveMinimum > exclusiveMaximum) {
            results.error(
                "Schema '$schemaName''exclusiveMinimum' ($exclusiveMinimum) cannot be greater than 'exclusiveMaximum' ($exclusiveMaximum).",
                asyncApiContext.getLine(node, node::exclusiveMinimum)
            )
        }
        // Warn when both inclusive and exclusive bounds are present.
        if (minimum != null && node.exclusiveMinimum != null) {
            results.warn(
                "Schema '$schemaName' defines both 'minimum' and 'exclusiveMinimum'. See Json Schema draft-07+ documentation.",
                asyncApiContext.getLine(node, node::exclusiveMinimum)
            )
        }
        if (maximum != null && node.exclusiveMaximum != null) {
            results.warn(
                "Schema '$schemaName' defines both 'maximum' and 'exclusiveMaximum'. See Json Schema draft-07+ documentation.",
                asyncApiContext.getLine(node, node::exclusiveMaximum)
            )
        }
        node.multipleOf?.let {
            if (it.toDouble() <= 0.0) {
                results.error(
                    "Schema '$schemaName' 'multipleOf' must be greater than zero.",
                    asyncApiContext.getLine(node, node::multipleOf)
                )
            }
        }
    }

    private fun validateStringLength(node: Schema, schemaName: String, results: ValidationResults) {
        val min = node.minLength?.toInt()
        val max = node.maxLength?.toInt()
        if (min != null && max != null && min > max) {
            results.error(
                "Schema '$schemaName' 'minLength' ($min) cannot be greater than 'maxLength' ($max).",
                asyncApiContext.getLine(node, node::minLength)
            )
        }
    }

    private fun validatePattern(node: Schema, schemaName: String, results: ValidationResults) {
        val pattern = node.pattern?.let(::sanitizeString) ?: return
        try {
            Regex(pattern)
        } catch (ex: Exception) {
            results.error(
                "Schema '$schemaName' invalid regex pattern in 'pattern': $pattern (${ex.message})",
                asyncApiContext.getLine(node, node::pattern)
            )
        }
    }

    private fun validateArray(node: Schema, schemaName: String, results: ValidationResults) {
        val minItems = node.minItems?.toInt()
        val maxItems = node.maxItems?.toInt()
        if (minItems != null && maxItems != null && minItems > maxItems) {
            results.error(
                "Schema '$schemaName' 'minItems' ($minItems) cannot be greater than 'maxItems' ($maxItems).",
                asyncApiContext.getLine(node, node::minItems)
            )
        }
    }

    private fun validateObject(node: Schema, schemaName: String, results: ValidationResults) {
        val minProps = node.minProperties?.toInt()
        val maxProps = node.maxProperties?.toInt()
        if (minProps != null && maxProps != null && minProps > maxProps) {
            results.error(
                "Schema '$schemaName' 'minProperties' ($minProps) cannot be greater than 'maxProperties' ($maxProps).",
                asyncApiContext.getLine(node, node::minProperties)
            )
        }
        val required = node.required?.map { item -> item.let(::sanitizeString) }
        required?.let {
            if (it.isEmpty()) {
                results.warn(
                    "Schema '$schemaName' defines an empty 'required' list — omit it if unused.",
                    asyncApiContext.getLine(node, node::required)
                )
            }
        }
    }

    private fun validateComposition(node: Schema, schemaName: String, results: ValidationResults) {
        val compositionCount = listOfNotNull(node.allOf, node.anyOf, node.oneOf).count { it.isNotEmpty() }
        if (compositionCount > 1) {
            results.warn(
                "Schema '$schemaName' uses multiple composition keywords ('allOf', 'anyOf', 'oneOf'). " +
                    "This can lead to ambiguous validation behavior.",
                asyncApiContext.getLine(node, node::allOf)
            )
        }
    }

    private fun validateDefaultValue(node: Schema, schemaName: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeAny)
            ?: return
        val type = node.type?.let(::sanitizeAny)
            ?: return
        if (!isDefaultCompatible(default, type)) {
            results.error(
                "Schema '$schemaName' default value '$default' does not match declared type '$type'.",
                asyncApiContext.getLine(node, node::default)
            )
        }
    }

    private fun validateDiscriminator(node: Schema, schemaName: String, results: ValidationResults) {
        val discriminator = node.discriminator?.let(::sanitizeString)
            ?: return
        val required = node.required?.map { item -> item.let(::sanitizeString) }
        val properties = node.properties?.keys?.map { key -> key.let(::sanitizeString) }
        required?.contains(discriminator)?.let {
            if (!it) {
                results.error(
                    "Schema '$schemaName' discriminator property '$discriminator' must be listed in 'required'.",
                    asyncApiContext.getLine(node, node::discriminator)
                )
            }
        }
        properties?.contains(discriminator)?.let {
            if (!it) {
                results.error(
                    "Schema '$schemaName' discriminator property '$discriminator' must exist in 'properties'.",
                    asyncApiContext.getLine(node, node::discriminator)
                )
            }
        }
    }

    private fun validateExternalDocs(node: Schema, schemaName: String, results: ValidationResults) {
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, schemaName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, "Schema ExternalDocs", results)

            null -> {}
        }
    }

    private fun validateBindings(node: Schema, schemaName: String, results: ValidationResults) {
        val bindings = node.bindings
            ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "Schema '$schemaName' defines an empty 'bindings' object.",
                asyncApiContext.getLine(node, node::bindings)
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, bindingName, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, "Schema Binding", results)
            }
        }
    }

    private fun isDefaultCompatible(default: Any?, type: Any): Boolean =
        when (type) {
            "string" -> default is String
            "number" -> default is Number
            "integer" -> default is Int || default is Long
            "boolean" -> default is Boolean
            "array" -> default is List<*>
            "object" -> default is Map<*, *>
            "null" -> default == null
            else -> true
        }
}
