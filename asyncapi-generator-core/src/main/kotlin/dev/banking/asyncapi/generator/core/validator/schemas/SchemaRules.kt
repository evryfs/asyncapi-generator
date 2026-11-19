package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ANNOTATION_IGNORED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_CONST_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEFAULT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DIALECT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_PROPERTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_KEYWORD_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_MULTIPLE_OF
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_NUMERIC_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNDECLARED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.UNION_INDISTINGUISHABLE_MEMBERS
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk
import java.math.BigDecimal

/**
 * Validates Schema Objects, boolean schemas and Multi Format Schema Objects, and walks the schemas nested in
 * them, their external documentation and their bindings.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaRules(
    private val asyncApiContext: AsyncApiContext,
) {
    private val propertyDeclarations = SchemaPropertyDeclarations(asyncApiContext)

    /** Checks a Schema Object, a boolean schema, a Multi Format Schema Object or a `$ref` to a schema. */
    fun checkInterface(
        schema: SchemaInterface,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        checkInterface(schema, path, walk, emptySet())
    }

    /** Checks the schemas of a `schemas` map, each named `Schema 'name'` below [path]. */
    fun checkMap(
        schemas: Map<String, SchemaInterface>,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        schemas.forEach { (name, schema) ->
            checkInterface(schema, path.named("Schema", name), walk, emptySet())
        }
    }

    /** Checks the schema [model] a reference led to. */
    fun checkModel(
        model: Any,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        when (model) {
            is Schema -> check(model, path, walk, emptySet())
            is MultiFormatSchema -> checkInterface(SchemaInterface.MultiFormatSchemaInline(model), path, walk)
            is SchemaInterface.BooleanSchema -> checkInterface(model, path, walk)
        }
    }

    private fun checkInterface(
        schemaInterface: SchemaInterface,
        path: ValidationPath,
        walk: ValidationWalk,
        enclosingDeclarations: Set<String>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                check(schemaInterface.schema, path, walk, enclosingDeclarations)

            is SchemaInterface.SchemaReference ->
                checkReference(schemaInterface.reference, path, walk)

            is SchemaInterface.MultiFormatSchemaInline -> walk.results.visit(schemaInterface.multiFormatSchema)
            is SchemaInterface.BooleanSchema -> walk.results.visit(schemaInterface)
        }
    }

    private fun check(
        node: Schema,
        path: ValidationPath,
        walk: ValidationWalk,
        enclosingDeclarations: Set<String>,
    ) {
        val results = walk.results
        if (!results.visit(node)) return
        val applicableDeclarations = enclosingDeclarations + propertyDeclarations.collect(node)
        validateKeywords(node, path, results)
        validateDialect(node, path, results)
        validateType(node, path, results)
        validateEnum(node, path, results)
        validateConst(node, path, results)
        validateNumericConstraints(node, path, results)
        validateStringLength(node, path, results)
        validateArray(node, path, results)
        validateObject(node, applicableDeclarations, path, results)
        validateDependencies(node, path, results)
        validateDefaultValue(node, path, results)
        validateDiscriminator(node, path, results)
        node.externalDocs?.let { walk.member(it, EXTERNAL_DOC, path.child("ExternalDocs")) }
        walk.bindings(node.bindings, path)

        // Recursive validation for nested schemas
        node.properties?.forEach { (name, subSchema) ->
            checkInterface(subSchema, path.named("Property", name), walk, emptySet())
        }
        node.patternProperties?.forEach { (pattern, subSchema) ->
            checkInterface(subSchema, path.named("Pattern property", pattern), walk, emptySet())
        }
        node.definitions?.forEach { (name, subSchema) ->
            checkInterface(subSchema, path.named("Definition", name), walk, emptySet())
        }
        node.items?.let { checkInterface(it, path, walk, emptySet()) }
        node.tupleItems?.forEachIndexed { index, subSchema ->
            checkInterface(subSchema, path.child("tuple item $index"), walk, emptySet())
        }
        node.additionalItems?.let { checkInterface(it, path, walk, emptySet()) }
        node.additionalProperties?.let { checkInterface(it, path, walk, emptySet()) }
        node.contains?.let { checkInterface(it, path, walk, emptySet()) }
        node.propertyNames?.let { checkInterface(it, path, walk, emptySet()) }
        node.dependencies?.forEach { (name, dependency) ->
            if (dependency is SchemaInterface) {
                checkInterface(dependency, path.named("dependency", name), walk, applicableDeclarations)
            }
        }

        node.allOf?.forEach { subSchema -> checkInterface(subSchema, path, walk, applicableDeclarations) }
        node.anyOf?.forEach { subSchema -> checkInterface(subSchema, path, walk, applicableDeclarations) }
        node.oneOf?.forEach { subSchema -> checkInterface(subSchema, path, walk, applicableDeclarations) }

        node.not?.let { checkInterface(it, path, walk, applicableDeclarations) }
        node.ifSchema?.let { checkInterface(it, path, walk, applicableDeclarations) }
        node.thenSchema?.let { checkInterface(it, path, walk, applicableDeclarations) }
        node.elseSchema?.let { checkInterface(it, path, walk, applicableDeclarations) }

        // Members are resolved by the recursion above.
        validateUnionMembers(node, path, results)
    }

    private fun checkReference(
        reference: Reference,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (!walk.results.visit(reference)) return
        validateKeywords(reference, path, walk.results)
        walk.reference(reference, SCHEMA, path)
    }

    private fun validateKeywords(
        node: Any,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        asyncApiContext.modelTracking.getFieldNames(node).forEach { keyword ->
            val classification = SchemaKeywordPolicy.classify(keyword)
            val severity = classification.severity ?: return@forEach
            val explanation = classification.explanation ?: return@forEach
            val sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, keyword)
            val message = "$path Schema Object keyword '$keyword' $explanation"
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
                    )

                WARNING ->
                    results.warn(
                        rule = rule,
                        message = message,
                        sourceLocation = sourceLocation,
                    )
            }
        }
    }

    private fun validateDialect(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if ("\$schema" !in asyncApiContext.modelTracking.getFieldNames(node)) return
        val dialect = node.schema
        if (dialect == null) {
            results.error(
                SCHEMA_DIALECT,
                "$path Schema Object keyword '\$schema' must contain a schema dialect URI.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, "\$schema"),
            )
            return
        }
        if (dialect.removeSuffix("#") in SUPPORTED_DRAFT_7_DIALECTS) return

        results.error(
            SCHEMA_DIALECT,
            "$path declares schema dialect '$dialect', but the generator supports AsyncAPI 3.0 Schema " +
                "Object semantics based on JSON Schema Draft 7. Remove '\$schema' or declare the Draft 7 dialect.",
            sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, "\$schema"),
        )
    }

    private fun validateType(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val type = node.type
        if (type == null) {
            if ("type" in asyncApiContext.modelTracking.getFieldNames(node)) {
                results.error(
                    SCHEMA_TYPE,
                    "$path Schema 'type' field must be a string or an array of strings. Found: null",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, "type"),
                )
            }
            return
        }
        val allowedTypes = setOf("string", "number", "integer", "boolean", "array", "object", "null")
        val typePath = path.child("Schema")
        when (type) {
            is String -> {
                if (type !in allowedTypes) {
                    results.error(
                        SCHEMA_TYPE,
                        "$typePath type '$type' is not valid. Must be one of: ${allowedTypes.joinToString()}",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                    )
                }
            }

            is List<*> -> {
                if (type.isEmpty()) {
                    results.error(
                        SCHEMA_TYPE_ARRAY_NONEMPTY,
                        "$typePath 'type' array must contain at least one type.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                    )
                }
                val typeList = type.filterIsInstance<String>()
                if (typeList.size != type.size) {
                    results.error(
                        SCHEMA_TYPE,
                        "$typePath all elements in 'type' array must be strings. Found non-string elements.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                    )
                }
                val invalidTypes = typeList.filter { it !in allowedTypes }
                if (invalidTypes.isNotEmpty()) {
                    results.error(
                        SCHEMA_TYPE,
                        "$typePath types ${invalidTypes.joinToString()} are not valid. Must be one " +
                            "of: ${allowedTypes.joinToString()}",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                    )
                }
                if (typeList.distinct().size != typeList.size) {
                    results.error(
                        SCHEMA_TYPE_ARRAY_UNIQUE,
                        "$typePath 'type' array must contain unique values.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                    )
                }
            }

            else -> {
                val invalidType = type::class.simpleName
                results.error(
                    SCHEMA_TYPE,
                    "$typePath 'type' field must be a string or an array of strings. Found: $invalidType",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                )
            }
        }
    }

    private fun validateEnum(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val enum = node.enum ?: return
        if (enum.isEmpty()) {
            results.error(
                SCHEMA_ENUM_NONEMPTY,
                "$path 'enum' must be a non-empty array of unique values.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::enum),
            )
        }
        if (SchemaValueSemantics.hasDuplicates(enum)) {
            results.error(
                SCHEMA_ENUM_UNIQUE,
                "$path 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::enum),
            )
        }
    }

    private fun validateConst(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (!node.constSet && node.const == null) return
        val const = node.const
        val type = node.type ?: return
        if (!SchemaValueSemantics.isCompatible(const, type)) {
            results.error(
                SCHEMA_CONST_TYPE,
                "$path 'const' value '$const' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::const),
            )
        }
    }

    private fun validateNumericConstraints(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        validateOrderedRange(
            node,
            "minimum",
            node.minimum,
            "maximum",
            node.maximum,
            SCHEMA_NUMERIC_RANGE,
            path,
            results,
        )
        validateOrderedRange(
            node,
            "exclusiveMinimum",
            node.exclusiveMinimum,
            "exclusiveMaximum",
            node.exclusiveMaximum,
            SCHEMA_NUMERIC_RANGE,
            path,
            results,
        )
        node.multipleOf?.let {
            if (SchemaValueSemantics.decimal(it)?.signum() != 1) {
                results.error(
                    SCHEMA_MULTIPLE_OF,
                    "$path 'multipleOf' must be greater than zero.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::multipleOf),
                )
            }
        }
    }

    private fun validateStringLength(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val minimum =
            validateNonNegativeInteger(
                node,
                "minLength",
                node.minLength,
                SCHEMA_STRING_LENGTH,
                path,
                results,
            )
        val maximum =
            validateNonNegativeInteger(
                node,
                "maxLength",
                node.maxLength,
                SCHEMA_STRING_LENGTH,
                path,
                results,
            )
        validateOrderedRange(
            node,
            "minLength",
            minimum,
            "maxLength",
            maximum,
            SCHEMA_STRING_LENGTH_RANGE,
            path,
            results,
        )
    }

    private fun validateArray(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val minimum =
            validateNonNegativeInteger(node, "minItems", node.minItems, SCHEMA_ARRAY_SIZE, path, results)
        val maximum =
            validateNonNegativeInteger(node, "maxItems", node.maxItems, SCHEMA_ARRAY_SIZE, path, results)
        validateOrderedRange(
            node,
            "minItems",
            minimum,
            "maxItems",
            maximum,
            SCHEMA_ARRAY_SIZE_RANGE,
            path,
            results,
        )
    }

    private fun validateObject(
        node: Schema,
        applicableDeclarations: Set<String>,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val minimum =
            validateNonNegativeInteger(
                node,
                "minProperties",
                node.minProperties,
                SCHEMA_OBJECT_SIZE,
                path,
                results,
            )
        val maximum =
            validateNonNegativeInteger(
                node,
                "maxProperties",
                node.maxProperties,
                SCHEMA_OBJECT_SIZE,
                path,
                results,
            )
        validateOrderedRange(
            node,
            "minProperties",
            minimum,
            "maxProperties",
            maximum,
            SCHEMA_OBJECT_SIZE_RANGE,
            path,
            results,
        )
        val required = node.required ?: return
        if (required.distinct().size != required.size) {
            results.error(
                SCHEMA_REQUIRED_UNIQUE,
                "$path 'required' contains duplicate property names.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::required),
            )
        }
        val missing = required.filter { it !in applicableDeclarations }
        if (missing.isNotEmpty()) {
            results.warn(
                SCHEMA_REQUIRED_UNDECLARED,
                "$path lists required properties $missing that are not declared in this object or its " +
                    "'allOf' composition. Define them explicitly, otherwise generation may fail.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::required),
            )
        }
    }

    private fun validateDependencies(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        node.dependencies?.forEach { (propertyName, dependency) ->
            val propertyNames = dependency as? List<*> ?: return@forEach
            val location = asyncApiContext.modelTracking.getSourceLocation(dependency)
            val duplicateIndex =
                propertyNames.indices.firstOrNull { index ->
                    propertyNames.subList(0, index).contains(propertyNames[index])
                }
            if (duplicateIndex != null) {
                results.error(
                    SCHEMA_DEPENDENCY_ARRAY_UNIQUE,
                    "$path dependency '$propertyName' contains duplicate property names.",
                    sourceLocation =
                        asyncApiContext.modelTracking.getElementLocation(dependency, duplicateIndex) ?: location,
                )
            }
        }
    }

    private fun validateDefaultValue(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (!node.defaultSet && node.default == null) return
        val default = node.default
        val type = node.type ?: return
        if (!SchemaValueSemantics.isCompatible(default, type)) {
            results.error(
                SCHEMA_DEFAULT_TYPE,
                "$path default value '$default' does not match declared type '$type'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::default),
            )
        }
    }

    private fun validateDiscriminator(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val discriminator = node.discriminator ?: return
        if (discriminator !in node.required.orEmpty()) {
            results.error(
                SCHEMA_DISCRIMINATOR_REQUIRED,
                "$path discriminator property '$discriminator' must be listed in 'required'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::discriminator),
            )
        }
        if (discriminator !in node.properties.orEmpty()) {
            results.error(
                SCHEMA_DISCRIMINATOR_PROPERTY,
                "$path discriminator property '$discriminator' must exist in 'properties'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::discriminator),
            )
        }
    }

    /**
     * Without a discriminator, generated code tells union members apart by the property names they
     * declare, so two members with the same names can never be told apart.
     */
    private fun validateUnionMembers(
        node: Schema,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.discriminator != null) return
        val members =
            (node.oneOf ?: node.anyOf)
                .orEmpty()
                .filterIsInstance<SchemaInterface.SchemaReference>()
                .mapNotNull { member ->
                    (member.reference.model as? Schema)
                        ?.takeIf { it.type.getPrimaryType() == "object" }
                        ?.let { member.reference.ref.substringAfterLast('/') to propertyNames(it) }
                }
        members
            .groupBy({ (_, propertyNames) -> propertyNames }, { (name, _) -> name })
            .values
            .firstOrNull { it.size > 1 }
            ?.let { names ->
                results.warn(
                    UNION_INDISTINGUISHABLE_MEMBERS,
                    "$path union members ${names.joinToString { "'$it'" }} declare the same property names " +
                        "and have no discriminator, so a payload cannot be told apart between them. " +
                        "Add a 'discriminator' or give the members different properties.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::oneOf),
                )
            }
    }

    private fun propertyNames(schema: Schema): Set<String> =
        schema.properties.orEmpty().keys +
            schema.allOf.orEmpty().flatMap { part ->
                when (part) {
                    is SchemaInterface.SchemaInline -> propertyNames(part.schema)
                    is SchemaInterface.SchemaReference ->
                        (part.reference.model as? Schema)
                            ?.let(
                                ::propertyNames,
                            ).orEmpty()
                    else -> emptySet()
                }
            }

    private fun validateNonNegativeInteger(
        node: Schema,
        keyword: String,
        value: Number?,
        rule: ValidationRule,
        path: ValidationPath,
        results: ValidationCollector,
    ): BigDecimal? {
        if (value == null) return null
        val decimal = SchemaValueSemantics.decimal(value)
        if (decimal == null || decimal.signum() < 0 || decimal.stripTrailingZeros().scale() > 0) {
            results.error(
                rule,
                "$path '$keyword' must be a non-negative integer. Found: $value.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, keyword),
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
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val lowerDecimal = lower?.let(SchemaValueSemantics::decimal) ?: return
        val upperDecimal = upper?.let(SchemaValueSemantics::decimal) ?: return
        if (lowerDecimal.compareTo(upperDecimal) > 0) {
            results.error(
                rule,
                "$path '$lowerKeyword' ($lower) cannot be greater than '$upperKeyword' ($upper) " +
                    "because the generator would emit contradictory constraints.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, lowerKeyword),
            )
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
