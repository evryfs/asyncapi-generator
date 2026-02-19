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

    fun validateInterface(schemaInterface: SchemaInterface, contextString: String, results: ValidationResults) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                validate(schemaInterface.schema, contextString, results)

            is SchemaInterface.SchemaReference ->
                referenceResolver.resolve(schemaInterface.reference, contextString, results)

            is SchemaInterface.MultiFormatSchemaInline -> {}
            is SchemaInterface.BooleanSchema -> {}
        }
    }

    fun validate(node: Schema, contextString: String, results: ValidationResults) {
        validateType(node, contextString, results)
        validateEnum(node, contextString, results)
        validateConst(node, contextString, results)
        validateNumericRange(node, contextString, results)
        validateStringLength(node, contextString, results)
        validatePattern(node, contextString, results)
        validateArray(node, contextString, results)
        validateObject(node, contextString, results)
        validateComposition(node, contextString, results)
        validateDefaultValue(node, contextString, results)
        validateDiscriminator(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)

        // Recursive validation for nested schemas
        node.properties?.forEach { (name, subSchema) -> validateInterface(subSchema, name, results) }
        node.definitions?.forEach { (name, subSchema) -> validateInterface(subSchema, name, results) }
        node.items?.let { validateInterface(it, contextString, results) }
        node.additionalItems?.let { validateInterface(it, contextString, results) }
        node.additionalProperties?.let { validateInterface(it, contextString, results) }
        node.contains?.let { validateInterface(it, contextString, results) }
        node.propertyNames?.let { validateInterface(it, contextString, results) }

        node.allOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results) }
        node.anyOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results) }
        node.oneOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results) }

        node.not?.let { subSchema -> validateInterface(subSchema, contextString, results) }
        node.ifSchema?.let { subSchema -> validateInterface(subSchema, contextString, results) }
        node.thenSchema?.let { subSchema -> validateInterface(subSchema, contextString, results) }
        node.elseSchema?.let { subSchema -> validateInterface(subSchema, contextString, results) }
    }

    private fun validateType(node: Schema, contextString: String, results: ValidationResults) {
        val type = node.type?.let(::sanitizeAny) ?: return
        val allowedTypes = setOf("string", "number", "integer", "boolean", "array", "object", "null")
        val contextString = "$contextString Schema"
        when (type) {
            is String -> {
                if (type.lowercase() !in allowedTypes) {
                    results.error(
                        "$contextString type '$type' is not valid. Must be one of: ${allowedTypes.joinToString()}",
                        asyncApiContext.getLine(node, node::type),
                        "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            is List<*> -> {
                val typeList = type.mapNotNull { item -> (item?.let(::sanitizeAny) as String).lowercase() }
                if (typeList.size != type.size) {
                    results.error(
                        "$contextString all elements in 'type' array must be strings. Found non-string elements.",
                        asyncApiContext.getLine(node, node::type),
                        "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
                val invalidTypes = typeList.filter { it !in allowedTypes }
                if (invalidTypes.isNotEmpty()) {
                    results.error(
                        "$contextString types ${invalidTypes.joinToString()} are not valid. Must be one " +
                            "of: ${allowedTypes.joinToString()}",
                        asyncApiContext.getLine(node, node::type),
                        "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            else -> {
                val invalidType = type::class.simpleName
                results.error(
                    "$contextString 'type' field must be a string or an array of strings. Found: $invalidType",
                    asyncApiContext.getLine(node, node::type),
                    "https://www.learnjsonschema.com/draft7/validation/type/",
                )
            }
        }
    }

    private fun validateEnum(node: Schema, contextString: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum?.let(::sanitizeAny) } ?: return
        if (enum.isEmpty()) {
            results.error(
                "$contextString 'enum' must be a non-empty array of unique values.",
                asyncApiContext.getLine(node, node::enum),
                "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
        if (enum.distinct().size != enum.size) {
            results.warn(
                "$contextString 'enum' contains duplicate values.",
                asyncApiContext.getLine(node, node::enum),
                "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
    }

    private fun validateConst(node: Schema, schemaName: String, results: ValidationResults) {
        val const = node.const?.let(::sanitizeAny) ?: return
        val type = node.type?.let(::sanitizeAny) ?: return
        if (!isDefaultCompatible(const, type)) {
            results.error(
                "$schemaName 'const' value '$const' does not match declared type '$type'.",
                asyncApiContext.getLine(node, node::const),
                "https://www.learnjsonschema.com/draft7/validation/const/"
            )
        }
    }

    private fun validateNumericRange(node: Schema, contextString: String, results: ValidationResults) {
        val minimum = node.minimum?.toDouble()
        val maximum = node.maximum?.toDouble()
        val exclusiveMinimum = node.exclusiveMinimum?.toDouble()
        val exclusiveMaximum = node.exclusiveMaximum?.toDouble()

        if (minimum != null && maximum != null && minimum > maximum) {
            results.error(
                "$contextString 'minimum' ($minimum) cannot be greater than 'maximum' ($maximum).",
                asyncApiContext.getLine(node, node::minimum),
                "https://www.learnjsonschema.com/draft7/validation/minimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maximum/"
            )
        }
        if (exclusiveMinimum != null && exclusiveMaximum != null && exclusiveMinimum > exclusiveMaximum) {
            results.error(
                "$contextString 'exclusiveMinimum' ($exclusiveMinimum) cannot be greater than " +
                    "'exclusiveMaximum' ($exclusiveMaximum).",
                asyncApiContext.getLine(node, node::exclusiveMinimum),
                "https://www.learnjsonschema.com/draft7/validation/exclusiveminimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusivemaximum/"
            )
        }
        // Warn when both inclusive and exclusive bounds are present.
        if (minimum != null && node.exclusiveMinimum != null) {
            results.warn(
                "$contextString defines both 'minimum' and 'exclusiveMinimum'. only 'minimum' will be used.",
                asyncApiContext.getLine(node, node::exclusiveMinimum),
                "https://www.learnjsonschema.com/draft7/validation/minimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusiveminimum/"
            )
        }
        if (maximum != null && node.exclusiveMaximum != null) {
            results.warn(
                "$contextString defines both 'maximum' and 'exclusiveMaximum'. only 'maximum' will be used.",
                asyncApiContext.getLine(node, node::exclusiveMaximum),
                "https://www.learnjsonschema.com/draft7/validation/maximum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusivemaximum/"
            )
        }
        node.multipleOf?.let {
            if (it.toDouble() <= 0.0) {
                results.error(
                    "$contextString 'multipleOf' must be greater than zero.",
                    asyncApiContext.getLine(node, node::multipleOf),
                    "https://www.learnjsonschema.com/draft7/validation/multipleof/"
                )
            }
        }
    }

    private fun validateStringLength(node: Schema, contextString: String, results: ValidationResults) {
        val min = node.minLength?.toInt()
        val max = node.maxLength?.toInt()
        if (min == null || max == null) return
        if (min < 0) {
            results.error(
                "$contextString 'minLength' ($min) cannot be a negative value.",
                asyncApiContext.getLine(node, node::minLength),
                "https://www.learnjsonschema.com/draft7/validation/minlength/",
            )
        }
        if (max < 0) {
            results.error(
                "$contextString 'maxLength' ($max) cannot be a negative value.",
                asyncApiContext.getLine(node, node::maxLength),
                "https://www.learnjsonschema.com/draft7/validation/maxlength/",
            )
        }
        if (min > max) {
            results.error(
                "$contextString 'minLength' ($min) cannot be greater than 'maxLength' ($max).",
                asyncApiContext.getLine(node, node::minLength),
                "https://www.learnjsonschema.com/draft7/validation/minlength/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maxlength/",
            )
        }
    }

    private fun validatePattern(node: Schema, contextString: String, results: ValidationResults) {
        val pattern = node.pattern?.let(::sanitizeString) ?: return
        try {
            Regex(pattern)
        } catch (ex: Exception) {
            results.error(
                "$contextString invalid regex pattern in 'pattern': $pattern (${ex.message})",
                asyncApiContext.getLine(node, node::pattern),
            )
        }
    }

    private fun validateArray(node: Schema, contextString: String, results: ValidationResults) {
        val minItems = node.minItems?.toInt()
        val maxItems = node.maxItems?.toInt()
        if (minItems == null || maxItems == null) return
        if (minItems < 0) {
            results.error(
                "$contextString 'minItems' ($minItems) cannot be a negative value.",
                asyncApiContext.getLine(node, node::minItems),
                "https://www.learnjsonschema.com/draft7/validation/minitems/"
            )
        }
        if (maxItems < 0) {
            results.error(
                "$contextString 'maxItems' ($maxItems) cannot be a negative value.",
                asyncApiContext.getLine(node, node::maxItems),
                "https://www.learnjsonschema.com/draft7/validation/maxitems/"
            )
        }
        if (minItems > maxItems) {
            results.error(
                "$contextString 'minItems' ($minItems) cannot be greater than 'maxItems' ($maxItems).",
                asyncApiContext.getLine(node, node::minItems),
                "https://www.learnjsonschema.com/draft7/validation/minitems/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maxitems/"
            )
        }
    }

    private fun validateObject(node: Schema, contextString: String, results: ValidationResults) {
        val minProps = node.minProperties?.toInt()
        val maxProps = node.maxProperties?.toInt()
        if (minProps != null && minProps < 0) {
            results.error(
                "$contextString 'minProperties' ($minProps) cannot be a negative value.",
                asyncApiContext.getLine(node, node::minProperties),
                "https://www.learnjsonschema.com/draft7/validation/minproperties/",
            )
        }
        if (maxProps != null && maxProps < 0) {
            results.error(
                "$contextString 'maxProperties' ($maxProps) cannot be a negative value.",
                asyncApiContext.getLine(node, node::maxProperties),
                "https://www.learnjsonschema.com/draft7/validation/maxproperties/",
            )
        }
        if (minProps != null && maxProps != null && minProps > maxProps) {
            results.error(
                "$contextString 'minProperties' ($minProps) cannot be greater than 'maxProperties' ($maxProps).",
                asyncApiContext.getLine(node, node::minProperties),
                "https://www.learnjsonschema.com/draft7/validation/minproperties/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maxproperties/",
            )
        }
        val required = node.required?.map { item -> item.let(::sanitizeString) } ?: return
        if (required.isEmpty()) {
            results.warn(
                "$contextString defines an empty 'required' list — omit it if unused.",
                asyncApiContext.getLine(node, node::required),
                "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
        if (required.distinct().size != required.size) {
            results.error(
                "$contextString 'required' contains duplicate property names.",
                asyncApiContext.getLine(node, node::required),
                "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
        val definedProperties = node.properties?.keys ?: emptySet()
        // This is a shallow check. It doesn't check 'allOf' or 'patternProperties'.
        // That's why we use a Warning, not an Error.
        val missing = required.filter { it !in definedProperties }
        if (missing.isNotEmpty()) {
            results.warn(
                "$contextString lists required properties $missing that are not defined in 'properties'. Ensure " +
                    "they are defined in 'allOf' or 'additionalProperties', otherwise generation may fail.",
                asyncApiContext.getLine(node, node::required),
                "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
    }

    private fun validateComposition(node: Schema, contextString: String, results: ValidationResults) {
        val compositionCount = listOfNotNull(node.allOf, node.anyOf, node.oneOf).count { it.isNotEmpty() }
        if (compositionCount > 1) {
            results.warn(
                "$contextString uses multiple composition keywords ('allOf', 'anyOf', 'oneOf'). This can lead to " +
                    "ambiguous validation behavior.",
                asyncApiContext.getLine(node, node::allOf),
            )
        }
    }

    private fun validateDefaultValue(node: Schema, contextString: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeAny) ?: return
        val type = node.type?.let(::sanitizeAny) ?: return
        if (!isDefaultCompatible(default, type)) {
            results.error(
                "$contextString default value '$default' does not match declared type '$type'.",
                asyncApiContext.getLine(node, node::default),
            )
        }
    }

    private fun validateDiscriminator(node: Schema, contextString: String, results: ValidationResults) {
        val discriminator = node.discriminator?.let(::sanitizeString) ?: return
        val required = node.required?.map { item -> item.let(::sanitizeString) }
        val properties = node.properties?.keys?.map { key -> key.let(::sanitizeString) }
        required?.contains(discriminator)?.let {
            if (!it) {
                results.error(
                    "$contextString discriminator property '$discriminator' must be listed in 'required'.",
                    asyncApiContext.getLine(node, node::discriminator),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                )
            }
        }
        properties?.contains(discriminator)?.let {
            if (!it) {
                results.error(
                    "$contextString discriminator property '$discriminator' must exist in 'properties'.",
                    asyncApiContext.getLine(node, node::discriminator),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                )
            }
        }
    }

    private fun validateExternalDocs(node: Schema, contextString: String, results: ValidationResults) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Schema, contextString: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }

    private fun isDefaultCompatible(value: Any?, type: Any?): Boolean {
        if (type == null) return true
        if (type is List<*>) {
            return type.any { isDefaultCompatible(value, it) }
        }
        return when (type.toString().lowercase()) {
            "string" -> value is String
            "number" -> value is Number
            "integer" -> value is Int || value is Long || (value is Number && value.toDouble() % 1.0 == 0.0)
            "boolean" -> value is Boolean
            "array" -> value is List<*>
            "object" -> value is Map<*, *>
            "null" -> value == null
            else -> true
        }
    }
}
