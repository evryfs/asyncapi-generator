package com.tietoevry.banking.asyncapi.generator.core.bundler.schemas

import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class SchemaBundler {

    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingsBundler = BindingBundler()

    fun bundleMap(schemas: Map<String, SchemaInterface>?, visited: Set<String>): Map<String, SchemaInterface>? =
        schemas?.mapValues { (_, schemaInterface) ->
            bundle(schemaInterface, visited)
        }

    fun bundleList(schemas: List<SchemaInterface>?, visited: Set<String>): List<SchemaInterface>? =
        schemas?.map { schemaInterface -> bundle(schemaInterface, visited) }

    /**
     * Bundles a Schema Interface, resolving and inlining references.
     *
     * @param schemaInterface The schema to bundle.
     * @param visited A set of reference strings (e.g. "#/components/schemas/A") that have already been visited
     *                in the current recursion branch. This is used to detect and break circular references
     *                (cycles), preventing StackOverflowErrors. If a reference is in this set, it will
     *                NOT be inlined, preserving the reference link instead.
     */
    fun bundle(schemaInterface: SchemaInterface?, visited: Set<String>): SchemaInterface =
        when (schemaInterface) {
            null ->
                throw IllegalArgumentException("Schema Interface $schemaInterface is not recognized")

            is SchemaInterface.SchemaInline ->
                SchemaInterface.SchemaInline(
                    bundleSchema(schemaInterface.schema, visited)
                )
            is SchemaInterface.SchemaReference -> {
                val ref = schemaInterface.reference.ref
                if (visited.contains(ref)) {
                    schemaInterface
                } else {
                    val model = schemaInterface.reference.requireModel<Schema>()
                    val newVisited = visited + ref
                    val bundled = bundleSchema(model, newVisited)
                    schemaInterface.reference.model = bundled
                    schemaInterface.reference.inline()
                    schemaInterface
                }
            }
            is SchemaInterface.MultiFormatSchemaInline ->
                schemaInterface
            is SchemaInterface.BooleanSchema ->
                schemaInterface
        }

    private fun bundleSchema(schema: Schema, visited: Set<String>): Schema {
        val bundledItems = schema.items?.let { bundle(it, visited) }
        val bundledAdditionalItems = schema.additionalItems?.let { bundle(it, visited) }
        val bundledContains = schema.contains?.let { bundle(it, visited) }

        val bundledProperties = bundleMap(schema.properties, visited)
        val bundledPatternProperties = bundleMap(schema.patternProperties, visited)
        val bundledAdditionalProperties = schema.additionalProperties?.let { bundle(it, visited) }
        val bundledPropertyNames = schema.propertyNames?.let { bundle(it, visited) }
        val bundledDefinitions = bundleMap(schema.definitions, visited)

        val bundledAllOf = bundleList(schema.allOf, visited)
        val bundledAnyOf = bundleList(schema.anyOf, visited)
        val bundledOneOf = bundleList(schema.oneOf, visited)
        val bundledNot = schema.not?.let { bundle(it, visited) }
        val bundledIf = schema.ifSchema?.let { bundle(it, visited) }
        val bundledThen = schema.thenSchema?.let { bundle(it, visited) }
        val bundledElse = schema.elseSchema?.let { bundle(it, visited) }

        val bundledExternalDocs = schema.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledBindings = bindingsBundler.bundleMap(schema.bindings, visited)
        return schema.copy(
            items = bundledItems,
            additionalItems = bundledAdditionalItems,
            contains = bundledContains,
            properties = bundledProperties,
            patternProperties = bundledPatternProperties,
            additionalProperties = bundledAdditionalProperties,
            propertyNames = bundledPropertyNames,
            definitions = bundledDefinitions,
            allOf = bundledAllOf,
            anyOf = bundledAnyOf,
            oneOf = bundledOneOf,
            not = bundledNot,
            ifSchema = bundledIf,
            thenSchema = bundledThen,
            elseSchema = bundledElse,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings,
        )
    }
}
