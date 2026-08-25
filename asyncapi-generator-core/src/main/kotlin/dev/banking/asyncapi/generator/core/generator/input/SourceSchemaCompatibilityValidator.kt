package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSourceSchemaFeature
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.Collections
import java.util.IdentityHashMap
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/** Validates recursive Schema Object features against source-generation capabilities. */
internal object SourceSchemaCompatibilityValidator {
    fun validate(
        output: String,
        schemas: Map<String, Schema>,
        checkStructuralModels: Boolean,
        checkJavaPatterns: Boolean,
    ) {
        val visited = Collections.newSetFromMap(IdentityHashMap<Schema, Boolean>())
        schemas.forEach { (schemaName, schema) ->
            validateSchema(
                output = output,
                rootSchemaName = schemaName,
                path = "$",
                schema = schema,
                checkStructuralModels = checkStructuralModels,
                checkJavaPatterns = checkJavaPatterns,
                visited = visited,
            )
        }
    }

    fun validateRootJavaPattern(
        output: String,
        rootSchemaName: String,
        schema: Schema,
    ) {
        validateJavaPattern(
            output = output,
            rootSchemaName = rootSchemaName,
            path = "$",
            schema = schema,
        )
    }

    private fun validateSchema(
        output: String,
        rootSchemaName: String,
        path: String,
        schema: Schema,
        checkStructuralModels: Boolean,
        checkJavaPatterns: Boolean,
        visited: MutableSet<Schema>,
    ) {
        if (!visited.add(schema)) return

        if (checkStructuralModels) {
            when {
                schema.tupleItems != null ->
                    reject(output, rootSchemaName, path, "tuple-form 'items'")

                schema.items is SchemaInterface.BooleanSchema && !schema.items.value ->
                    reject(output, rootSchemaName, path, "'items: false'")

                schema.type == null && schema.enum?.any { value -> value !is String } == true ->
                    reject(
                        output,
                        rootSchemaName,
                        path,
                        "an enum without 'type' that contains non-string values",
                    )
            }
        }

        if (checkJavaPatterns) {
            validateJavaPattern(output, rootSchemaName, path, schema)
        }

        schema.items?.let { child ->
            validateInterface(output, rootSchemaName, "$path.items", child, checkStructuralModels, checkJavaPatterns, visited)
        }
        schema.tupleItems?.forEachIndexed { index, child ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.items[$index]",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.additionalItems?.let { child ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.additionalItems",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.contains?.let { child ->
            validateInterface(output, rootSchemaName, "$path.contains", child, checkStructuralModels, checkJavaPatterns, visited)
        }
        schema.properties?.forEach { (name, child) ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.properties['$name']",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.patternProperties?.forEach { (pattern, child) ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.patternProperties['$pattern']",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.additionalProperties?.let { child ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.additionalProperties",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.propertyNames?.let { child ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.propertyNames",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        schema.dependencies?.forEach { (name, child) ->
            if (child is SchemaInterface) {
                validateInterface(
                    output,
                    rootSchemaName,
                    "$path.dependencies['$name']",
                    child,
                    checkStructuralModels,
                    checkJavaPatterns,
                    visited,
                )
            }
        }
        schema.definitions?.forEach { (name, child) ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.definitions['$name']",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
        validateList(output, rootSchemaName, path, "allOf", schema.allOf, checkStructuralModels, checkJavaPatterns, visited)
        validateList(output, rootSchemaName, path, "anyOf", schema.anyOf, checkStructuralModels, checkJavaPatterns, visited)
        validateList(output, rootSchemaName, path, "oneOf", schema.oneOf, checkStructuralModels, checkJavaPatterns, visited)
        schema.not?.let { child ->
            validateInterface(output, rootSchemaName, "$path.not", child, checkStructuralModels, checkJavaPatterns, visited)
        }
        schema.ifSchema?.let { child ->
            validateInterface(output, rootSchemaName, "$path.if", child, checkStructuralModels, checkJavaPatterns, visited)
        }
        schema.thenSchema?.let { child ->
            validateInterface(output, rootSchemaName, "$path.then", child, checkStructuralModels, checkJavaPatterns, visited)
        }
        schema.elseSchema?.let { child ->
            validateInterface(output, rootSchemaName, "$path.else", child, checkStructuralModels, checkJavaPatterns, visited)
        }
    }

    private fun validateList(
        output: String,
        rootSchemaName: String,
        path: String,
        field: String,
        schemas: List<SchemaInterface>?,
        checkStructuralModels: Boolean,
        checkJavaPatterns: Boolean,
        visited: MutableSet<Schema>,
    ) {
        schemas?.forEachIndexed { index, child ->
            validateInterface(
                output,
                rootSchemaName,
                "$path.$field[$index]",
                child,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
    }

    private fun validateInterface(
        output: String,
        rootSchemaName: String,
        path: String,
        schema: SchemaInterface,
        checkStructuralModels: Boolean,
        checkJavaPatterns: Boolean,
        visited: MutableSet<Schema>,
    ) {
        if (schema is SchemaInterface.SchemaInline) {
            validateSchema(
                output,
                rootSchemaName,
                path,
                schema.schema,
                checkStructuralModels,
                checkJavaPatterns,
                visited,
            )
        }
    }

    private fun validateJavaPattern(
        output: String,
        rootSchemaName: String,
        path: String,
        schema: Schema,
    ) {
        schema.pattern?.let { pattern ->
            try {
                Pattern.compile(pattern)
            } catch (exception: PatternSyntaxException) {
                reject(
                    output,
                    rootSchemaName,
                    path,
                    "a 'pattern' that Java cannot compile: ${exception.description}",
                )
            }
        }
    }

    private fun reject(
        output: String,
        rootSchemaName: String,
        path: String,
        feature: String,
    ): Nothing =
        throw UnsupportedSourceSchemaFeature(
            output = output,
            rootSchemaName = rootSchemaName,
            schemaPath = path,
            feature = feature,
        )
}
