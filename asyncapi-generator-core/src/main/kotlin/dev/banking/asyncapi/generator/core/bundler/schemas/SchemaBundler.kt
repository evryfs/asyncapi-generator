package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.componentSchemaNameOrNull
import dev.banking.asyncapi.generator.core.model.references.componentSchemaReference
import dev.banking.asyncapi.generator.core.model.references.isExternalReference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles schema objects and references.
 */
internal class SchemaBundler {
    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingsBundler = BindingBundler()

    fun bundleMap(
        schemas: Map<String, SchemaInterface>?,
        scope: BundlingScope,
    ): Map<String, SchemaInterface>? =
        schemas?.mapValues { (_, schemaInterface) ->
            bundle(schemaInterface, scope)
        }

    internal fun bundleComponentMap(
        schemas: Map<String, SchemaInterface>?,
        scope: BundlingScope,
    ): Map<String, SchemaInterface>? =
        schemas?.mapValues { (name, schemaInterface) ->
            bundle(schemaInterface, scope.defineRootSchema(name))
        }

    fun bundleList(
        schemas: List<SchemaInterface>?,
        scope: BundlingScope,
    ): List<SchemaInterface>? = schemas?.map { schemaInterface -> bundle(schemaInterface, scope) }

    fun bundle(
        schemaInterface: SchemaInterface,
        scope: BundlingScope,
    ): SchemaInterface =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                SchemaInterface.SchemaInline(
                    bundleSchema(schemaInterface.schema, scope),
                )
            is SchemaInterface.SchemaReference ->
                bundleReference(schemaInterface, scope)
            is SchemaInterface.MultiFormatSchemaInline ->
                schemaInterface
            is SchemaInterface.BooleanSchema ->
                schemaInterface
        }

    /**
     * Bundles a schema field of a binding. A reference that leaves the root document is promoted to
     * `components.schemas` under its pointer name, so the schema keeps the name it has in the source
     * contract and code generated from the bundle matches code generated from the original files.
     */
    fun bundleBindingField(
        schemaInterface: SchemaInterface,
        scope: BundlingScope,
    ): SchemaInterface {
        if (schemaInterface is SchemaInterface.SchemaReference) {
            val reference = schemaInterface.reference
            val referencedModel = reference.requireModel<Any>()
            if (referencedModel !is SchemaInterface.BooleanSchema &&
                (reference.ref.isExternalReference() || scope.externalDocumentScope)
            ) {
                return bundlePromotedReference(reference, referencedModel, scope)
            }
        }
        return bundle(schemaInterface, scope)
    }

    private fun bundleReference(
        schemaInterface: SchemaInterface.SchemaReference,
        scope: BundlingScope,
    ): SchemaInterface {
        val reference = schemaInterface.reference
        val referencedModel = reference.requireModel<Any>()
        if (referencedModel is SchemaInterface.BooleanSchema) {
            return referencedModel
        }
        val keepAsReference = isComponentSchemaRef(reference.ref)

        if (shouldPromote(reference, referencedModel, scope)) {
            return bundlePromotedReference(reference, referencedModel, scope)
        }

        if (scope.hasVisited(reference)) {
            return if (keepAsReference) {
                schemaInterface
            } else {
                referencedModel.toInlineSchemaInterface()
            }
        }

        if (keepAsReference) {
            // A component schema of the root document: the bundle keeps the `$ref`, and the component
            // itself is bundled with the root components.
            when (referencedModel) {
                is Schema ->
                    return schemaInterface.copy(
                        reference = reference.copy(model = bundleSchema(referencedModel, scope.enter(reference))),
                    )
                is MultiFormatSchema -> return schemaInterface
                else ->
                    throw IllegalArgumentException("Schema reference ${reference.ref} resolved to unsupported model")
            }
        }

        return when (referencedModel) {
            is Schema ->
                SchemaInterface.SchemaInline(
                    bundleSchema(
                        schema = referencedModel,
                        scope =
                            if (reference.ref.isExternalReference()) {
                                scope.enterExternalSchema(reference)
                            } else {
                                scope.enter(reference)
                            },
                    ),
                )
            is MultiFormatSchema ->
                SchemaInterface.MultiFormatSchemaInline(referencedModel)
            else ->
                throw IllegalArgumentException("Schema reference ${reference.ref} resolved to unsupported model")
        }
    }

    private fun shouldPromote(
        reference: Reference,
        referencedModel: Any,
        scope: BundlingScope,
    ): Boolean =
        (reference.ref.isExternalReference() || scope.externalDocumentScope) &&
            (scope.run.schemaRecursion.isRecursive(referencedModel) || isComponentSchemaRef(reference.ref))

    private fun bundlePromotedReference(
        reference: Reference,
        referencedModel: Any,
        scope: BundlingScope,
    ): SchemaInterface {
        val entry = scope.run.schemaPromotions.reserve(reference, referencedModel)
        val definesPromotedSchema = scope.definesRootSchema(entry.name)

        if (scope.run.schemaPromotions.startBundling(entry)) {
            val bundledSchema =
                when (referencedModel) {
                    is Schema ->
                        SchemaInterface.SchemaInline(
                            bundleSchema(
                                schema = referencedModel,
                                scope = scope.enterExternalSchema(reference),
                            ),
                        )

                    is MultiFormatSchema ->
                        SchemaInterface.MultiFormatSchemaInline(referencedModel)

                    else ->
                        throw IllegalArgumentException(
                            "Schema reference ${reference.ref} resolved to unsupported model",
                        )
                }
            scope.run.schemaPromotions.complete(entry, bundledSchema)
        }

        if (definesPromotedSchema) {
            return checkNotNull(entry.bundledSchema) {
                "Root schema '${entry.name}' was not bundled"
            }
        }

        return SchemaInterface.SchemaReference(
            Reference(
                ref = componentSchemaReference(entry.name),
                // The bundled schema, so generation sees what the bundled document contains; a recursive
                // schema still being bundled has no bundled form yet and points at its source model.
                model = entry.bundledSchema?.bundledModel() ?: entry.model,
                referenceCategoryKey = reference.referenceCategoryKey ?: ReferenceCategoryKey.SCHEMA,
            ),
        )
    }

    private fun SchemaInterface.bundledModel(): Any? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.MultiFormatSchemaInline -> multiFormatSchema
            else -> null
        }

    private fun Any.toInlineSchemaInterface(): SchemaInterface =
        when (this) {
            is Schema -> SchemaInterface.SchemaInline(this)
            is MultiFormatSchema -> SchemaInterface.MultiFormatSchemaInline(this)
            else -> throw IllegalArgumentException("Schema reference resolved to unsupported model")
        }

    private fun bundleSchema(
        schema: Schema,
        scope: BundlingScope,
    ): Schema {
        val bundledItems = schema.items?.let { bundle(it, scope) }
        val bundledTupleItems = bundleList(schema.tupleItems, scope)
        val bundledAdditionalItems = schema.additionalItems?.let { bundle(it, scope) }
        val bundledContains = schema.contains?.let { bundle(it, scope) }

        val bundledProperties = bundleMap(schema.properties, scope)
        val bundledPatternProperties = bundleMap(schema.patternProperties, scope)
        val bundledAdditionalProperties = schema.additionalProperties?.let { bundle(it, scope) }
        val bundledPropertyNames = schema.propertyNames?.let { bundle(it, scope) }
        val bundledDependencies =
            schema.dependencies?.mapValues { (_, dependency) ->
                if (dependency is SchemaInterface) bundle(dependency, scope) else dependency
            }
        val bundledDefinitions = bundleMap(schema.definitions, scope)

        val bundledAllOf = bundleList(schema.allOf, scope)
        val bundledAnyOf = bundleList(schema.anyOf, scope)
        val bundledOneOf = bundleList(schema.oneOf, scope)
        val bundledNot = schema.not?.let { bundle(it, scope) }
        val bundledIf = schema.ifSchema?.let { bundle(it, scope) }
        val bundledThen = schema.thenSchema?.let { bundle(it, scope) }
        val bundledElse = schema.elseSchema?.let { bundle(it, scope) }

        val bundledExternalDocs = schema.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledBindings = bindingsBundler.bundleMap(schema.bindings, scope)
        return schema.copy(
            items = bundledItems,
            tupleItems = bundledTupleItems,
            additionalItems = bundledAdditionalItems,
            contains = bundledContains,
            properties = bundledProperties,
            patternProperties = bundledPatternProperties,
            additionalProperties = bundledAdditionalProperties,
            propertyNames = bundledPropertyNames,
            dependencies = bundledDependencies,
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

    private fun isComponentSchemaRef(ref: String): Boolean = ref.componentSchemaNameOrNull() != null
}
