package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles schemas: rebuilds an inline schema from its bundled members and hands a schema reference to
 * [SchemaReferenceBundler], which keeps, inlines or promotes it.
 *
 * @param walk bundles the external docs and bindings of a schema
 */
internal class SchemaBundler(
    private val walk: BundlingWalk,
) {
    private val references = SchemaReferenceBundler(this)

    /** Bundles one schema. */
    fun bundle(
        schemaInterface: SchemaInterface,
        scope: BundlingScope,
    ): SchemaInterface =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                SchemaInterface.SchemaInline(
                    bundleMembers(schemaInterface.schema, scope),
                )
            is SchemaInterface.SchemaReference -> references.bundle(schemaInterface, scope)
            is SchemaInterface.MultiFormatSchemaInline -> schemaInterface
            is SchemaInterface.BooleanSchema -> schemaInterface
        }

    /** Bundles the component schemas of the root document; each one defines the root schema of its name. */
    fun bundleComponentMap(
        schemas: Map<String, SchemaInterface>?,
        scope: BundlingScope,
    ): Map<String, SchemaInterface>? =
        schemas?.mapValues { (name, schemaInterface) ->
            bundle(schemaInterface, scope.defineRootSchema(name))
        }

    /**
     * Bundles a schema field of a binding. A reference that leaves the root document is promoted to
     * `components.schemas` under its pointer name, so the schema keeps the name it has in the source
     * contract and code generated from the bundle matches code generated from the original files.
     */
    fun bundleBindingField(
        schemaInterface: SchemaInterface,
        scope: BundlingScope,
    ): SchemaInterface =
        if (schemaInterface is SchemaInterface.SchemaReference) {
            references.bundleBindingField(schemaInterface, scope)
        } else {
            bundle(schemaInterface, scope)
        }

    /** Returns [schema] with every member schema, its external docs and its bindings bundled. */
    fun bundleMembers(
        schema: Schema,
        scope: BundlingScope,
    ): Schema =
        schema.copy(
            items = schema.items?.let { bundle(it, scope) },
            tupleItems = schema.tupleItems?.map { bundle(it, scope) },
            additionalItems = schema.additionalItems?.let { bundle(it, scope) },
            contains = schema.contains?.let { bundle(it, scope) },
            properties = schema.properties?.mapValues { (_, property) -> bundle(property, scope) },
            patternProperties = schema.patternProperties?.mapValues { (_, property) -> bundle(property, scope) },
            additionalProperties = schema.additionalProperties?.let { bundle(it, scope) },
            propertyNames = schema.propertyNames?.let { bundle(it, scope) },
            dependencies =
                schema.dependencies?.mapValues { (_, dependency) ->
                    if (dependency is SchemaInterface) bundle(dependency, scope) else dependency
                },
            definitions = schema.definitions?.mapValues { (_, definition) -> bundle(definition, scope) },
            allOf = schema.allOf?.map { bundle(it, scope) },
            anyOf = schema.anyOf?.map { bundle(it, scope) },
            oneOf = schema.oneOf?.map { bundle(it, scope) },
            not = schema.not?.let { bundle(it, scope) },
            ifSchema = schema.ifSchema?.let { bundle(it, scope) },
            thenSchema = schema.thenSchema?.let { bundle(it, scope) },
            elseSchema = schema.elseSchema?.let { bundle(it, scope) },
            externalDocs = walk.member(schema.externalDocs, walk.externalDoc, scope),
            bindings = walk.members(schema.bindings, walk.binding, scope),
        )
}
