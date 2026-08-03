package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ANNOTATION_IGNORED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_CONST_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEFAULT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_ITEMS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DIALECT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_PROPERTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ITEMS_REPRESENTATION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_KEYWORD_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_MULTIPLE_OF
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_NUMERIC_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_PATTERN
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNDECLARED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_UNTYPED_ENUM
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import java.math.BigDecimal
import java.util.Collections
import java.util.IdentityHashMap
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal class SchemaValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val bindingValidator = BindingValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)
    private val propertyDeclarations = SchemaPropertyDeclarations(asyncApiContext)

    fun validateInterface(schemaInterface: SchemaInterface, contextString: String, results: ValidationCollector) {
        validateInterface(schemaInterface, contextString, results, newVisitedSet(), emptySet())
    }

    fun validateMap(
        schemas: Map<String, SchemaInterface>,
        contextString: String,
        results: ValidationCollector,
    ) {
        val visited = newVisitedSet()
        schemas.forEach { (name, schema) ->
            validateInterface(schema, "$contextString Schema '$name'", results, visited, emptySet())
        }
    }

    private fun validateInterface(
        schemaInterface: SchemaInterface,
        contextString: String,
        results: ValidationCollector,
        visited: MutableSet<Any>,
        enclosingDeclarations: Set<String>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                validate(schemaInterface.schema, contextString, results, visited, enclosingDeclarations)

            is SchemaInterface.SchemaReference ->
                validateReference(schemaInterface.reference, contextString, results, visited)

            is SchemaInterface.MultiFormatSchemaInline -> results.visit(schemaInterface.multiFormatSchema)
            is SchemaInterface.BooleanSchema -> results.visit(schemaInterface)
        }
    }

    fun validate(node: Schema, contextString: String, results: ValidationCollector) {
        validate(node, contextString, results, newVisitedSet(), emptySet())
    }

    private fun validate(
        node: Schema,
        contextString: String,
        results: ValidationCollector,
        visited: MutableSet<Any>,
        enclosingDeclarations: Set<String>,
    ) {
        if (!visited.add(node) || !results.visit(node)) {
            return
        }
        val applicableDeclarations = enclosingDeclarations + propertyDeclarations.collect(node)
        validateKeywords(node, contextString, results)
        validateDialect(node, contextString, results)
        validateKeywordRepresentations(node, contextString, results)
        validateType(node, contextString, results)
        validateEnum(node, contextString, results)
        validateConst(node, contextString, results)
        validateNumericConstraints(node, contextString, results)
        validateStringLength(node, contextString, results)
        validatePattern(node, contextString, results)
        validateArray(node, contextString, results)
        validateObject(node, applicableDeclarations, contextString, results)
        validateDependencies(node, contextString, results)
        validateDefaultValue(node, contextString, results)
        validateDiscriminator(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)

        // Recursive validation for nested schemas
        node.properties?.forEach { (name, subSchema) ->
            validateInterface(subSchema, name, results, visited, emptySet())
        }
        node.patternProperties?.forEach { (pattern, subSchema) ->
            validateInterface(subSchema, "Pattern property '$pattern'", results, visited, emptySet())
        }
        node.definitions?.forEach { (name, subSchema) ->
            validateInterface(subSchema, name, results, visited, emptySet())
        }
        node.items?.let { validateInterface(it, contextString, results, visited, emptySet()) }
        node.tupleItems?.forEachIndexed { index, subSchema ->
            validateInterface(subSchema, "$contextString tuple item $index", results, visited, emptySet())
        }
        node.additionalItems?.let { validateInterface(it, contextString, results, visited, emptySet()) }
        node.additionalProperties?.let { validateInterface(it, contextString, results, visited, emptySet()) }
        node.contains?.let { validateInterface(it, contextString, results, visited, emptySet()) }
        node.propertyNames?.let { validateInterface(it, contextString, results, visited, emptySet()) }
        node.dependencies?.forEach { (name, dependency) ->
            if (dependency is SchemaInterface) {
                validateInterface(
                    dependency,
                    "$contextString dependency '$name'",
                    results,
                    visited,
                    applicableDeclarations,
                )
            }
        }

        node.allOf?.forEach { subSchema ->
            validateInterface(subSchema, contextString, results, visited, applicableDeclarations)
        }
        node.anyOf?.forEach { subSchema ->
            validateInterface(subSchema, contextString, results, visited, applicableDeclarations)
        }
        node.oneOf?.forEach { subSchema ->
            validateInterface(subSchema, contextString, results, visited, applicableDeclarations)
        }

        node.not?.let { validateInterface(it, contextString, results, visited, applicableDeclarations) }
        node.ifSchema?.let { validateInterface(it, contextString, results, visited, applicableDeclarations) }
        node.thenSchema?.let { validateInterface(it, contextString, results, visited, applicableDeclarations) }
        node.elseSchema?.let { validateInterface(it, contextString, results, visited, applicableDeclarations) }
    }

    private fun validateReference(
        reference: Reference,
        contextString: String,
        results: ValidationCollector,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(reference)) {
            return
        }
        validateKeywords(reference, contextString, results)
        referenceResolver.resolve(reference, SCHEMA, contextString, results)
    }

    private fun newVisitedSet(): MutableSet<Any> =
        Collections.newSetFromMap(IdentityHashMap())

    private fun validateKeywords(node: Any, contextString: String, results: ValidationCollector) {
        asyncApiContext.getFieldNames(node).forEach { keyword ->
            val classification = SchemaKeywordPolicy.classify(keyword)
            val severity = classification.severity ?: return@forEach
            val explanation = classification.explanation ?: return@forEach
            val sourceLocation = asyncApiContext.getSourceLocation(node, keyword)
            val message = "$contextString Schema Object keyword '$keyword' $explanation"
            val rule: ValidationRule =
                if (classification.category == SchemaKeywordCategory.IGNORED_ANNOTATION) {
                    SCHEMA_ANNOTATION_IGNORED
                } else {
                    SCHEMA_KEYWORD_UNSUPPORTED
                }

            when (severity) {
                ERROR ->
                    results.error(
                        rule = rule,
                        message = message,
                        sourceLocation = sourceLocation,
                        doc = classification.doc,
                    )

                WARNING ->
                    results.warn(
                        rule = rule,
                        message = message,
                        sourceLocation = sourceLocation,
                        doc = classification.doc,
                    )
            }
        }
    }

    private fun validateKeywordRepresentations(node: Schema, contextString: String, results: ValidationCollector) {
        if ("items" in asyncApiContext.getFieldNames(node)) {
            val items = asyncApiContext.getFieldValue(node, "items")
            when {
                node.tupleItems != null ->
                    results.error(
                        SCHEMA_ITEMS_REPRESENTATION,
                        "$contextString uses tuple-form 'items', which is valid Draft 7 but cannot be represented " +
                            "safely by the Java and Kotlin generators. Use a single Schema Object in 'items'.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, "items"),
                        doc = "https://www.learnjsonschema.com/draft7/applicator/items/",
                    )

                node.items is SchemaInterface.BooleanSchema && !node.items.value ->
                    results.error(
                        SCHEMA_ITEMS_REPRESENTATION,
                        "$contextString uses 'items: false', which cannot be represented safely by the Java and " +
                            "Kotlin collection types.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, "items"),
                        doc = "https://www.learnjsonschema.com/draft7/applicator/items/",
                    )

                items !is Map<*, *> && items !is Boolean ->
                    results.error(
                        SCHEMA_ITEMS_REPRESENTATION,
                        "$contextString Schema Object keyword 'items' must contain a Schema Object or a boolean schema.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, "items"),
                        doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
                    )
            }
        }

    }

    private fun validateDialect(node: Schema, contextString: String, results: ValidationCollector) {
        if ("\$schema" !in asyncApiContext.getFieldNames(node)) return
        val dialect = node.schema
        if (dialect == null) {
            results.error(
                SCHEMA_DIALECT,
                "$contextString Schema Object keyword '\$schema' must contain a schema dialect URI.",
                sourceLocation = asyncApiContext.getSourceLocation(node, "\$schema"),
                doc = "https://json-schema.org/draft-07/schema",
            )
            return
        }
        if (dialect.removeSuffix("#") in SUPPORTED_DRAFT_7_DIALECTS) return

        results.error(
            SCHEMA_DIALECT,
            "$contextString declares schema dialect '$dialect', but the generator supports AsyncAPI 3.0 Schema " +
                "Object semantics based on JSON Schema Draft 7. Remove '\$schema' or declare the Draft 7 dialect.",
            sourceLocation = asyncApiContext.getSourceLocation(node, "\$schema"),
            doc = "https://json-schema.org/draft-07/schema",
        )
    }

    private fun validateType(node: Schema, contextString: String, results: ValidationCollector) {
        val type = node.type
        if (type == null) {
            if ("type" in asyncApiContext.getFieldNames(node)) {
                results.error(
                    SCHEMA_TYPE,
                    "$contextString Schema 'type' field must be a string or an array of strings. Found: null",
                    sourceLocation = asyncApiContext.getSourceLocation(node, "type"),
                    doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                )
            }
            return
        }
        val allowedTypes = setOf("string", "number", "integer", "boolean", "array", "object", "null")
        val contextString = "$contextString Schema"
        when (type) {
            is String -> {
                if (type !in allowedTypes) {
                    results.error(
                        SCHEMA_TYPE,
                        "$contextString type '$type' is not valid. Must be one of: ${allowedTypes.joinToString()}",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            is List<*> -> {
                if (type.isEmpty()) {
                    results.error(
                        SCHEMA_TYPE_ARRAY_NONEMPTY,
                        "$contextString 'type' array must contain at least one type.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
                val typeList = type.filterIsInstance<String>()
                if (typeList.size != type.size) {
                    results.error(
                        SCHEMA_TYPE,
                        "$contextString all elements in 'type' array must be strings. Found non-string elements.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
                val invalidTypes = typeList.filter { it !in allowedTypes }
                if (invalidTypes.isNotEmpty()) {
                    results.error(
                        SCHEMA_TYPE,
                        "$contextString types ${invalidTypes.joinToString()} are not valid. Must be one " +
                            "of: ${allowedTypes.joinToString()}",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
                if (typeList.distinct().size != typeList.size) {
                    results.error(
                        SCHEMA_TYPE_ARRAY_UNIQUE,
                        "$contextString 'type' array must contain unique values.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                        doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                    )
                }
            }

            else -> {
                val invalidType = type::class.simpleName
                results.error(
                    SCHEMA_TYPE,
                    "$contextString 'type' field must be a string or an array of strings. Found: $invalidType",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                    doc = "https://www.learnjsonschema.com/draft7/validation/type/",
                )
            }
        }
    }

    private fun validateEnum(node: Schema, contextString: String, results: ValidationCollector) {
        val enum = node.enum ?: return
        if (enum.isEmpty()) {
            results.error(
                SCHEMA_ENUM_EMPTY,
                "$contextString 'enum' must be a non-empty array of unique values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
                doc = "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
        if (SchemaValueSemantics.hasDuplicates(enum)) {
            results.error(
                SCHEMA_ENUM_UNIQUE,
                "$contextString 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
                doc = "https://www.learnjsonschema.com/draft7/validation/enum/",
            )
        }
        if (node.type == null && enum.isNotEmpty() && enum.any { it !is String }) {
            results.error(
                SCHEMA_UNTYPED_ENUM,
                "$contextString has an enum without 'type' that contains non-string values. The generator can " +
                    "infer only an all-string enum safely; declare the intended type explicitly.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
            )
        }
    }

    private fun validateConst(node: Schema, schemaName: String, results: ValidationCollector) {
        if (!node.constSet && node.const == null) return
        val const = node.const
        val type = node.type ?: return
        if (!SchemaValueSemantics.isCompatible(const, type)) {
            results.error(
                SCHEMA_CONST_TYPE,
                "$schemaName 'const' value '$const' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::const),
                doc = "https://www.learnjsonschema.com/draft7/validation/const/",
            )
        }
    }

    private fun validateNumericConstraints(node: Schema, contextString: String, results: ValidationCollector) {
        validateOrderedRange(
            node,
            "minimum",
            node.minimum,
            "maximum",
            node.maximum,
            SCHEMA_NUMERIC_RANGE,
            contextString,
            results,
        )
        validateOrderedRange(
            node,
            "exclusiveMinimum",
            node.exclusiveMinimum,
            "exclusiveMaximum",
            node.exclusiveMaximum,
            SCHEMA_NUMERIC_RANGE,
            contextString,
            results,
        )
        node.multipleOf?.let {
            if (SchemaValueSemantics.decimal(it)?.signum() != 1) {
                results.error(
                    SCHEMA_MULTIPLE_OF,
                    "$contextString 'multipleOf' must be greater than zero.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::multipleOf),
                    doc = "https://www.learnjsonschema.com/draft7/validation/multipleof/",
                )
            }
        }
    }

    private fun validateStringLength(node: Schema, contextString: String, results: ValidationCollector) {
        val minimum = validateNonNegativeInteger(
            node,
            "minLength",
            node.minLength,
            SCHEMA_STRING_LENGTH,
            contextString,
            results,
        )
        val maximum = validateNonNegativeInteger(
            node,
            "maxLength",
            node.maxLength,
            SCHEMA_STRING_LENGTH,
            contextString,
            results,
        )
        validateOrderedRange(
            node,
            "minLength",
            minimum,
            "maxLength",
            maximum,
            SCHEMA_STRING_LENGTH_RANGE,
            contextString,
            results,
        )
    }

    private fun validatePattern(node: Schema, contextString: String, results: ValidationCollector) {
        val pattern = node.pattern ?: return
        try {
            Pattern.compile(pattern)
        } catch (ex: PatternSyntaxException) {
            results.error(
                SCHEMA_PATTERN,
                "$contextString 'pattern' cannot be compiled by the Java regular-expression engine used by " +
                    "generated Jakarta Validation constraints: ${ex.description}.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::pattern),
            )
        }
    }

    private fun validateArray(node: Schema, contextString: String, results: ValidationCollector) {
        val minimum =
            validateNonNegativeInteger(node, "minItems", node.minItems, SCHEMA_ARRAY_SIZE, contextString, results)
        val maximum =
            validateNonNegativeInteger(node, "maxItems", node.maxItems, SCHEMA_ARRAY_SIZE, contextString, results)
        validateOrderedRange(
            node,
            "minItems",
            minimum,
            "maxItems",
            maximum,
            SCHEMA_ARRAY_SIZE_RANGE,
            contextString,
            results,
        )
    }

    private fun validateObject(
        node: Schema,
        applicableDeclarations: Set<String>,
        contextString: String,
        results: ValidationCollector,
    ) {
        val minimum = validateNonNegativeInteger(
            node,
            "minProperties",
            node.minProperties,
            SCHEMA_OBJECT_SIZE,
            contextString,
            results,
        )
        val maximum = validateNonNegativeInteger(
            node,
            "maxProperties",
            node.maxProperties,
            SCHEMA_OBJECT_SIZE,
            contextString,
            results,
        )
        validateOrderedRange(
            node,
            "minProperties",
            minimum,
            "maxProperties",
            maximum,
            SCHEMA_OBJECT_SIZE_RANGE,
            contextString,
            results,
        )
        val required = node.required ?: return
        if (required.isEmpty()) {
            results.warn(
                SCHEMA_REQUIRED_EMPTY,
                "$contextString defines an empty 'required' list — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
        if (required.distinct().size != required.size) {
            results.error(
                SCHEMA_REQUIRED_UNIQUE,
                "$contextString 'required' contains duplicate property names.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
        val missing = required.filter { it !in applicableDeclarations }
        if (missing.isNotEmpty()) {
            results.warn(
                SCHEMA_REQUIRED_UNDECLARED,
                "$contextString lists required properties $missing that are not declared in this object or its " +
                    "'allOf' composition. Define them explicitly, otherwise generation may fail.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::required),
                doc = "https://www.learnjsonschema.com/draft7/validation/required/",
            )
        }
    }

    private fun validateDependencies(node: Schema, contextString: String, results: ValidationCollector) {
        node.dependencies?.forEach { (propertyName, dependency) ->
            val propertyNames = dependency as? List<*> ?: return@forEach
            val location = asyncApiContext.getSourceLocation(dependency)
            if (propertyNames.isEmpty()) {
                results.error(
                    SCHEMA_DEPENDENCY_ARRAY_NONEMPTY,
                    "$contextString dependency '$propertyName' must contain at least one property name.",
                    sourceLocation = location,
                    doc = JSON_SCHEMA_DEPENDENCIES_DOC,
                )
            }

            val nonStringIndex = propertyNames.indexOfFirst { it !is String }
            if (nonStringIndex >= 0) {
                results.error(
                    SCHEMA_DEPENDENCY_ARRAY_ITEMS,
                    "$contextString dependency '$propertyName' must contain only property names.",
                    sourceLocation = asyncApiContext.getSourceLocation(dependency, "[$nonStringIndex]") ?: location,
                    doc = JSON_SCHEMA_DEPENDENCIES_DOC,
                )
            }

            val duplicateIndex = propertyNames.indices.firstOrNull { index ->
                propertyNames.subList(0, index).contains(propertyNames[index])
            }
            if (duplicateIndex != null) {
                results.error(
                    SCHEMA_DEPENDENCY_ARRAY_UNIQUE,
                    "$contextString dependency '$propertyName' contains duplicate property names.",
                    sourceLocation = asyncApiContext.getSourceLocation(dependency, "[$duplicateIndex]") ?: location,
                    doc = JSON_SCHEMA_DEPENDENCIES_DOC,
                )
            }
        }
    }

    private fun validateDefaultValue(node: Schema, contextString: String, results: ValidationCollector) {
        if (!node.defaultSet && node.default == null) return
        val default = node.default
        val type = node.type ?: return
        if (!SchemaValueSemantics.isCompatible(default, type)) {
            results.error(
                SCHEMA_DEFAULT_TYPE,
                "$contextString default value '$default' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
            )
        }
    }

    private fun validateDiscriminator(node: Schema, contextString: String, results: ValidationCollector) {
        val discriminator = node.discriminator ?: return
        if (discriminator !in node.required.orEmpty()) {
            results.error(
                SCHEMA_DISCRIMINATOR_REQUIRED,
                "$contextString discriminator property '$discriminator' must be listed in 'required'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::discriminator),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
            )
        }
        if (discriminator !in node.properties.orEmpty()) {
            results.error(
                SCHEMA_DISCRIMINATOR_PROPERTY,
                "$contextString discriminator property '$discriminator' must exist in 'properties'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::discriminator),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject",
            )
        }
    }

    private fun validateExternalDocs(node: Schema, contextString: String, results: ValidationCollector) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, EXTERNAL_DOC, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Schema, contextString: String, results: ValidationCollector) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, BINDING, contextString, results)
            }
        }
    }

    private fun validateNonNegativeInteger(
        node: Schema,
        keyword: String,
        value: Number?,
        rule: ValidationRule,
        contextString: String,
        results: ValidationCollector,
    ): BigDecimal? {
        if (value == null) return null
        val decimal = SchemaValueSemantics.decimal(value)
        if (decimal == null || decimal.signum() < 0 || decimal.stripTrailingZeros().scale() > 0) {
            results.error(
                rule,
                "$contextString '$keyword' must be a non-negative integer. Found: $value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, keyword),
                doc = "https://json-schema.org/draft-07/draft-handrews-json-schema-validation-01#rfc.section.6",
            )
            return null
        }
        return decimal
    }

    private fun validateOrderedRange(
        node: Schema,
        lowerKeyword: String,
        lower: Number?,
        upperKeyword: String,
        upper: Number?,
        rule: ValidationRule,
        contextString: String,
        results: ValidationCollector,
    ) {
        val lowerDecimal = lower?.let(SchemaValueSemantics::decimal) ?: return
        val upperDecimal = upper?.let(SchemaValueSemantics::decimal) ?: return
        if (lowerDecimal.compareTo(upperDecimal) > 0) {
            results.error(
                rule,
                "$contextString '$lowerKeyword' ($lower) cannot be greater than '$upperKeyword' ($upper) " +
                    "because the generator would emit contradictory constraints.",
                sourceLocation = asyncApiContext.getSourceLocation(node, lowerKeyword),
            )
        }
    }

    private companion object {
        const val JSON_SCHEMA_DEPENDENCIES_DOC =
            "https://json-schema.org/draft-07/draft-handrews-json-schema-validation-01#rfc.section.6.5.7"

        val SUPPORTED_DRAFT_7_DIALECTS =
            setOf(
                "http://json-schema.org/draft-07/schema",
                "https://json-schema.org/draft-07/schema",
            )
    }
}
