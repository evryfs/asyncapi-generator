package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Supplies the string type required by code generation for untyped, all-string enums.
 *
 * JSON Schema does not require an enum to declare a type, so this inference belongs
 * to generation analysis rather than parsing or semantic conformance validation.
 */
class EnumTypeAnalyzer : AnalysisStage<Map<String, Schema>> {

    override fun analyze(schemas: Map<String, Schema>): Map<String, Schema> =
        schemas.mapValues { (_, schema) -> analyzeSchema(schema) }

    private fun analyzeSchema(schema: Schema): Schema =
        schema.copy(
            type = schema.type ?: inferredStringType(schema),
            items = schema.items?.let(::analyzeInterface),
            tupleItems = schema.tupleItems?.map(::analyzeInterface),
            additionalItems = schema.additionalItems?.let(::analyzeInterface),
            contains = schema.contains?.let(::analyzeInterface),
            properties = schema.properties?.mapValues { (_, value) -> analyzeInterface(value) },
            patternProperties = schema.patternProperties?.mapValues { (_, value) -> analyzeInterface(value) },
            additionalProperties = schema.additionalProperties?.let(::analyzeInterface),
            propertyNames = schema.propertyNames?.let(::analyzeInterface),
            dependencies = schema.dependencies?.mapValues { (_, value) ->
                if (value is SchemaInterface) analyzeInterface(value) else value
            },
            definitions = schema.definitions?.mapValues { (_, value) -> analyzeInterface(value) },
            allOf = schema.allOf?.map(::analyzeInterface),
            anyOf = schema.anyOf?.map(::analyzeInterface),
            oneOf = schema.oneOf?.map(::analyzeInterface),
            not = schema.not?.let(::analyzeInterface),
            ifSchema = schema.ifSchema?.let(::analyzeInterface),
            thenSchema = schema.thenSchema?.let(::analyzeInterface),
            elseSchema = schema.elseSchema?.let(::analyzeInterface),
        )

    private fun inferredStringType(schema: Schema): String? =
        "string".takeIf {
            !schema.enum.isNullOrEmpty() && schema.enum.all { value -> value is String }
        }

    private fun analyzeInterface(schema: SchemaInterface): SchemaInterface =
        when (schema) {
            is SchemaInterface.SchemaInline -> SchemaInterface.SchemaInline(analyzeSchema(schema.schema))
            is SchemaInterface.BooleanSchema,
            is SchemaInterface.MultiFormatSchemaInline,
            is SchemaInterface.SchemaReference,
            -> schema
        }
}
