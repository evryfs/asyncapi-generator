package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSourceSchemaFeature
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.schemas.childSchemasByPath
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
            SchemaWalk(output, schemaName, checkStructuralModels, checkJavaPatterns, visited).validate("$", schema)
        }
    }

    fun validateRootJavaPattern(
        output: String,
        rootSchemaName: String,
        schema: Schema,
    ) {
        validateJavaPattern(output, rootSchemaName, "$", schema)
    }

    /**
     * Validates one root schema and every inline schema it holds, each schema once.
     *
     * @param output the generated output the schemas are checked for, as named in errors
     * @param rootSchemaName the root schema, as named in errors
     * @param checkStructuralModels whether shapes that no model class can represent are rejected
     * @param checkJavaPatterns whether every `pattern` must compile as a Java regular expression
     * @param visited the schemas already validated, shared by every root
     */
    private class SchemaWalk(
        private val output: String,
        private val rootSchemaName: String,
        private val checkStructuralModels: Boolean,
        private val checkJavaPatterns: Boolean,
        private val visited: MutableSet<Schema>,
    ) {
        fun validate(
            path: String,
            schema: Schema,
        ) {
            if (!visited.add(schema)) return
            if (checkStructuralModels) validateStructure(path, schema)
            if (checkJavaPatterns) validateJavaPattern(output, rootSchemaName, path, schema)
            schema.childSchemasByPath().forEach { (childPath, child) ->
                if (child is SchemaInterface.SchemaInline) validate(path + childPath, child.schema)
            }
        }

        private fun validateStructure(
            path: String,
            schema: Schema,
        ) {
            val forbiddenProperty =
                schema.properties
                    ?.entries
                    ?.firstOrNull { (_, child) -> child.isFalseSchema() }
                    ?.key
            when {
                schema.tupleItems != null ->
                    reject(output, rootSchemaName, path, "tuple-form 'items'")

                schema.items?.isFalseSchema() == true ->
                    reject(output, rootSchemaName, path, "'items: false'")

                forbiddenProperty != null ->
                    reject(
                        output,
                        rootSchemaName,
                        "$path.properties['$forbiddenProperty']",
                        "a property schema 'false', which no value satisfies",
                    )

                schema.type == null && schema.enum?.any { value -> value !is String } == true ->
                    reject(output, rootSchemaName, path, "an enum without 'type' that contains non-string values")
            }
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
                reject(output, rootSchemaName, path, "a 'pattern' that Java cannot compile: ${exception.description}")
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

    /** True for the schema `false`, which no value satisfies. */
    private fun SchemaInterface.isFalseSchema(): Boolean = this is SchemaInterface.BooleanSchema && !value
}
