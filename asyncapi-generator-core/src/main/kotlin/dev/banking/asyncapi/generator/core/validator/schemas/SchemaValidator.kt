package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeAny
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString
import java.util.Collections
import java.util.IdentityHashMap

class SchemaValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val bindingValidator = BindingValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(schemaInterface: SchemaInterface, contextString: String, results: ValidationResults) {
        validateInterface(schemaInterface, contextString, results, newVisitedSet())
    }

    fun validateMap(
        schemas: Map<String, SchemaInterface>,
        contextString: String,
        results: ValidationResults,
    ) {
        val visited = newVisitedSet()
        schemas.forEach { (name, schema) ->
            validateInterface(schema, "$contextString Schema '$name'", results, visited)
        }
    }

    private fun validateInterface(
        schemaInterface: SchemaInterface,
        contextString: String,
        results: ValidationResults,
        visited: MutableSet<Any>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                validate(schemaInterface.schema, contextString, results, visited)

            is SchemaInterface.SchemaReference ->
                validateReference(schemaInterface.reference, contextString, results, visited)

            is SchemaInterface.MultiFormatSchemaInline -> {}
            is SchemaInterface.BooleanSchema -> {}
        }
    }

    fun validate(node: Schema, contextString: String, results: ValidationResults) {
        validate(node, contextString, results, newVisitedSet())
    }

    private fun validate(
        node: Schema,
        contextString: String,
        results: ValidationResults,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(node)) {
            return
        }
        validateKeywords(node, contextString, results)
        validateDialect(node, contextString, results)
        validateKeywordRepresentations(node, contextString, results)
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
        node.properties?.forEach { (name, subSchema) ->
            validateInterface(subSchema, name, results, visited)
        }
        node.definitions?.forEach { (name, subSchema) ->
            validateInterface(subSchema, name, results, visited)
        }
        node.items?.let { validateInterface(it, contextString, results, visited) }
        node.additionalItems?.let { validateInterface(it, contextString, results, visited) }
        node.additionalProperties?.let { validateInterface(it, contextString, results, visited) }
        node.contains?.let { validateInterface(it, contextString, results, visited) }
        node.propertyNames?.let { validateInterface(it, contextString, results, visited) }

        node.allOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results, visited) }
        node.anyOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results, visited) }
        node.oneOf?.forEach { subSchema -> validateInterface(subSchema, contextString, results, visited) }

        node.not?.let { subSchema -> validateInterface(subSchema, contextString, results, visited) }
        node.ifSchema?.let { subSchema -> validateInterface(subSchema, contextString, results, visited) }
        node.thenSchema?.let { subSchema -> validateInterface(subSchema, contextString, results, visited) }
        node.elseSchema?.let { subSchema -> validateInterface(subSchema, contextString, results, visited) }
    }

    private fun validateReference(
        reference: Reference,
        contextString: String,
        results: ValidationResults,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(reference)) {
            return
        }
        validateKeywords(reference, contextString, results)
        referenceResolver.resolve(reference, contextString, results)
        when (val resolved = reference.model) {
            is Schema -> validate(resolved, contextString, results, visited)
            is Reference -> validateReference(resolved, contextString, results, visited)
            is SchemaInterface ->
                validateInterface(resolved, contextString, results, visited)
        }
    }

    private fun newVisitedSet(): MutableSet<Any> =
        Collections.newSetFromMap(IdentityHashMap())

    private fun validateKeywords(node: Any, contextString: String, results: ValidationResults) {
        asyncApiContext.getFieldNames(node).forEach { keyword ->
            val classification = SchemaKeywordPolicy.classify(keyword)
            val severity = classification.severity ?: return@forEach
            val explanation = classification.explanation ?: return@forEach
            val sourceLocation = asyncApiContext.getSourceLocation(node, keyword)
            val message = "$contextString Schema Object keyword '$keyword' $explanation"

            when (severity) {
                ERROR ->
                    results.error(
                        message = message,
                        sourceLocation = sourceLocation,
                        doc = classification.doc,
                    )

                WARNING ->
                    results.warn(
                        message = message,
                        sourceLocation = sourceLocation,
                        doc = classification.doc,
                    )
            }
        }
    }

    private fun validateKeywordRepresentations(node: Schema, contextString: String, results: ValidationResults) {
        if ("items" in asyncApiContext.getFieldNames(node)) {
            val items = asyncApiContext.getFieldValue(node, "items")
            when {
                items is List<*> ->
                    results.error(
                        "$contextString Schema Object keyword 'items' does not support tuple validation. Use a " +
                            "single Schema Object in 'items'.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, "items"),
                        doc = "https://www.learnjsonschema.com/draft7/applicator/items/",
                    )

                items !is Map<*, *> && items !is Boolean ->
                    results.error(
                        "$contextString Schema Object keyword 'items' must contain a Schema Object or a boolean schema.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, "items"),
                        doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                    )
            }
        }

        listOf("exclusiveMinimum", "exclusiveMaximum").forEach { keyword ->
            if (keyword in asyncApiContext.getFieldNames(node) &&
                asyncApiContext.getFieldValue(node, keyword) !is Number
            ) {
                results.error(
                    "$contextString Schema Object keyword '$keyword' must contain a numeric boundary.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, keyword),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                )
            }
        }

        if ("discriminator" in asyncApiContext.getFieldNames(node) &&
            asyncApiContext.getFieldValue(node, "discriminator") !is String
        ) {
            results.error(
                "$contextString Schema Object keyword 'discriminator' must contain the name of a required " +
                    "string property.",
                sourceLocation = asyncApiContext.getSourceLocation(node, "discriminator"),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
            )
        }
    }

    private fun validateDialect(node: Schema, contextString: String, results: ValidationResults) {
        if ("\$schema" !in asyncApiContext.getFieldNames(node)) return
        val dialect = node.schema
        if (dialect == null) {
            results.error(
                "$contextString Schema Object keyword '\$schema' must contain a schema dialect URI.",
                sourceLocation = asyncApiContext.getSourceLocation(node, "\$schema"),
                doc = "https://json-schema.org/draft-07/schema",
            )
            return
        }
        if (dialect.removeSuffix("#") in SUPPORTED_DRAFT_7_DIALECTS) return

        results.error(
            "$contextString declares schema dialect '$dialect', but the generator supports AsyncAPI 3.0 Schema " +
                "Object semantics based on JSON Schema Draft 7. Remove '\$schema' or declare the Draft 7 dialect.",
            sourceLocation = asyncApiContext.getSourceLocation(node, "\$schema"),
            doc = "https://json-schema.org/draft-07/schema",
        )
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
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            is List<*> -> {
                val typeList = type.mapNotNull { item -> (item?.let(::sanitizeAny) as String).lowercase() }
                if (typeList.size != type.size) {
                    results.error(
                        "$contextString all elements in 'type' array must be strings. Found non-string elements.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
                val invalidTypes = typeList.filter { it !in allowedTypes }
                if (invalidTypes.isNotEmpty()) {
                    results.error(
                        "$contextString types ${invalidTypes.joinToString()} are not valid. Must be one " +
                            "of: ${allowedTypes.joinToString()}",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            else -> {
                val invalidType = type::class.simpleName
                results.error(
                    "$contextString 'type' field must be a string or an array of strings. Found: $invalidType",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                    doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                )
            }
        }
    }

    private fun validateEnum(node: Schema, contextString: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum?.let(::sanitizeAny) } ?: return
        if (enum.isEmpty()) {
            results.error(
                "$contextString 'enum' must be a non-empty array of unique values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
                doc = "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
        if (enum.distinct().size != enum.size) {
            results.warn(
                "$contextString 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
                doc = "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
    }

    private fun validateConst(node: Schema, schemaName: String, results: ValidationResults) {
        val const = node.const?.let(::sanitizeAny) ?: return
        val type = node.type?.let(::sanitizeAny) ?: return
        if (!isDefaultCompatible(const, type)) {
            results.error(
                "$schemaName 'const' value '$const' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::const),
                doc = "https://www.learnjsonschema.com/draft7/validation/const/",
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
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minimum),
                doc = "https://www.learnjsonschema.com/draft7/validation/minimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maximum/",
            )
        }
        if (exclusiveMinimum != null && exclusiveMaximum != null && exclusiveMinimum > exclusiveMaximum) {
            results.error(
                "$contextString 'exclusiveMinimum' ($exclusiveMinimum) cannot be greater than " +
                    "'exclusiveMaximum' ($exclusiveMaximum).",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::exclusiveMinimum),
                doc = "https://www.learnjsonschema.com/draft7/validation/exclusiveminimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusivemaximum/",
            )
        }
        // Warn when both inclusive and exclusive bounds are present.
        if (minimum != null && node.exclusiveMinimum != null) {
            results.warn(
                "$contextString defines both 'minimum' and 'exclusiveMinimum'. only 'minimum' will be used.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::exclusiveMinimum),
                doc = "https://www.learnjsonschema.com/draft7/validation/minimum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusiveminimum/",
            )
        }
        if (maximum != null && node.exclusiveMaximum != null) {
            results.warn(
                "$contextString defines both 'maximum' and 'exclusiveMaximum'. only 'maximum' will be used.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::exclusiveMaximum),
                doc = "https://www.learnjsonschema.com/draft7/validation/maximum/, " +
                    "https://www.learnjsonschema.com/draft7/validation/exclusivemaximum/",
            )
        }
        node.multipleOf?.let {
            if (it.toDouble() <= 0.0) {
                results.error(
                    "$contextString 'multipleOf' must be greater than zero.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::multipleOf),
                    doc = "https://www.learnjsonschema.com/draft7/validation/multipleof/",
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
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minLength),
                doc = "https://www.learnjsonschema.com/draft7/validation/minlength/",
            )
        }
        if (max < 0) {
            results.error(
                "$contextString 'maxLength' ($max) cannot be a negative value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::maxLength),
                doc = "https://www.learnjsonschema.com/draft7/validation/maxlength/",
            )
        }
        if (min > max) {
            results.error(
                "$contextString 'minLength' ($min) cannot be greater than 'maxLength' ($max).",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minLength),
                doc = "https://www.learnjsonschema.com/draft7/validation/minlength/, " +
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
                sourceLocation = asyncApiContext.getSourceLocation(node, node::pattern),
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
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minItems),
                doc = "https://www.learnjsonschema.com/draft7/validation/minitems/",
            )
        }
        if (maxItems < 0) {
            results.error(
                "$contextString 'maxItems' ($maxItems) cannot be a negative value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::maxItems),
                doc = "https://www.learnjsonschema.com/draft7/validation/maxitems/",
            )
        }
        if (minItems > maxItems) {
            results.error(
                "$contextString 'minItems' ($minItems) cannot be greater than 'maxItems' ($maxItems).",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minItems),
                doc = "https://www.learnjsonschema.com/draft7/validation/minitems/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maxitems/",
            )
        }
    }

    private fun validateObject(node: Schema, contextString: String, results: ValidationResults) {
        val minProps = node.minProperties?.toInt()
        val maxProps = node.maxProperties?.toInt()
        if (minProps != null && minProps < 0) {
            results.error(
                "$contextString 'minProperties' ($minProps) cannot be a negative value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minProperties),
                doc = "https://www.learnjsonschema.com/draft7/validation/minproperties/",
            )
        }
        if (maxProps != null && maxProps < 0) {
            results.error(
                "$contextString 'maxProperties' ($maxProps) cannot be a negative value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::maxProperties),
                doc = "https://www.learnjsonschema.com/draft7/validation/maxproperties/",
            )
        }
        if (minProps != null && maxProps != null && minProps > maxProps) {
            results.error(
                "$contextString 'minProperties' ($minProps) cannot be greater than 'maxProperties' ($maxProps).",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::minProperties),
                doc = "https://www.learnjsonschema.com/draft7/validation/minproperties/, " +
                    "https://www.learnjsonschema.com/draft7/validation/maxproperties/",
            )
        }
        val required = node.required?.map { item -> item.let(::sanitizeString) } ?: return
        node.properties?.forEach { (propName, propSchema) ->
            if (propName in required) {
                val schema = (propSchema as? SchemaInterface.SchemaInline)?.schema
                if (schema != null && schema.defaultSet && schema.default == null) {
                    results.error(
                        "$contextString property '$propName' is required but has default: null.",
                        sourceLocation = asyncApiContext.getSourceLocation(schema, schema::default),
                    )
                }
            }
        }
        if (required.isEmpty()) {
            results.warn(
                "$contextString defines an empty 'required' list — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
        if (required.distinct().size != required.size) {
            results.error(
                "$contextString 'required' contains duplicate property names.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
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
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
    }

    private fun validateComposition(node: Schema, contextString: String, results: ValidationResults) {
        val compositionCount = listOfNotNull(node.allOf, node.anyOf, node.oneOf).count { it.isNotEmpty() }
        if (compositionCount > 1) {
            results.warn(
                "$contextString uses multiple composition keywords ('allOf', 'anyOf', 'oneOf'). This can lead to " +
                    "ambiguous validation behavior.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::allOf),
            )
        }
    }

    private fun validateDefaultValue(node: Schema, contextString: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeAny) ?: return
        val type = node.type?.let(::sanitizeAny) ?: return
        if (!isDefaultCompatible(default, type)) {
            results.error(
                "$contextString default value '$default' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
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
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::discriminator),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                )
            }
        }
        properties?.contains(discriminator)?.let {
            if (!it) {
                results.error(
                    "$contextString discriminator property '$discriminator' must exist in 'properties'.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::discriminator),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
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

    private companion object {
        val SUPPORTED_DRAFT_7_DIALECTS =
            setOf(
                "http://json-schema.org/draft-07/schema",
                "https://json-schema.org/draft-07/schema",
            )
    }
}
