package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.componentSchemaReference
import dev.banking.asyncapi.generator.core.model.references.componentSchemaNameOrNull
import dev.banking.asyncapi.generator.core.model.references.isExternalReference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles schema objects and references.
 *
 * Expected behavior is covered by:
 * - `SchemaBundlerTest`
 */
class SchemaBundler {

    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingsBundler = BindingBundler()
    fun bundleMap(schemas: Map<String, SchemaInterface>?, visited: Set<String>): Map<String, SchemaInterface>? =
        bundleMap(schemas, BundlingContext.from(visited))

    fun bundleMap(schemas: Map<String, SchemaInterface>?, context: BundlingContext): Map<String, SchemaInterface>? =
        schemas?.mapValues { (_, schemaInterface) ->
            bundle(schemaInterface, context)
        }

    internal fun bundleComponentMap(
        schemas: Map<String, SchemaInterface>?,
        context: BundlingContext,
    ): Map<String, SchemaInterface>? =
        schemas?.mapValues { (name, schemaInterface) ->
            bundle(schemaInterface, context.defineRootSchema(name))
        }

    fun bundleList(schemas: List<SchemaInterface>?, visited: Set<String>): List<SchemaInterface>? =
        bundleList(schemas, BundlingContext.from(visited))

    fun bundleList(schemas: List<SchemaInterface>?, context: BundlingContext): List<SchemaInterface>? =
        schemas?.map { schemaInterface -> bundle(schemaInterface, context) }

    fun bundle(schemaInterface: SchemaInterface?, visited: Set<String>): SchemaInterface =
        bundle(schemaInterface, BundlingContext.from(visited))

    fun bundle(schemaInterface: SchemaInterface?, context: BundlingContext): SchemaInterface =
        when (schemaInterface) {
            null ->
                throw IllegalArgumentException("Schema Interface $schemaInterface is not recognized")

            is SchemaInterface.SchemaInline ->
                SchemaInterface.SchemaInline(
                    bundleSchema(schemaInterface.schema, context)
                )
            is SchemaInterface.SchemaReference ->
                bundleReference(schemaInterface, context)
            is SchemaInterface.MultiFormatSchemaInline ->
                schemaInterface
            is SchemaInterface.BooleanSchema ->
                schemaInterface
        }

    private fun bundleReference(
        schemaInterface: SchemaInterface.SchemaReference,
        context: BundlingContext,
    ): SchemaInterface {
        val reference = schemaInterface.reference
        val keepAsReference = isComponentSchemaRef(reference.ref)
        val referencedModel = reference.requireModel<Any>()

        if (shouldPromote(reference, referencedModel, context)) {
            return bundlePromotedReference(reference, referencedModel, context)
        }

        if (context.hasVisited(reference)) {
            return if (keepAsReference) {
                schemaInterface
            } else {
                referencedModel.toInlineSchemaInterface()
            }
        }

        if (keepAsReference) {
            when (referencedModel) {
                is Schema ->
                    ReferenceBundler.bundleReferencedModel<Schema>(
                        reference = reference,
                        context = context,
                    ) { schema, nextContext ->
                        bundleSchema(
                            schema = schema,
                            context = if (reference.ref.isExternalReference()) {
                                context.enterExternalSchema(reference)
                            } else {
                                nextContext
                            },
                        )
                    }
                is MultiFormatSchema ->
                    ReferenceBundler.inlineIfUnvisited(reference, context)
                else ->
                    throw IllegalArgumentException("Schema reference ${reference.ref} resolved to unsupported model")
            }
            return schemaInterface
        }

        return when (referencedModel) {
            is Schema ->
                SchemaInterface.SchemaInline(
                    bundleSchema(
                        schema = referencedModel,
                        context = if (reference.ref.isExternalReference()) {
                            context.enterExternalSchema(reference)
                        } else {
                            context.enter(reference)
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
        context: BundlingContext,
    ): Boolean =
        (reference.ref.isExternalReference() || context.externalSchemaScope) &&
            context.schemaRecursion.isRecursive(referencedModel)

    private fun bundlePromotedReference(
        reference: Reference,
        referencedModel: Any,
        context: BundlingContext,
    ): SchemaInterface {
        val entry = context.schemaPromotions.reserve(reference, referencedModel)
        val definesPromotedSchema = context.definesRootSchema(entry.name)

        if (context.schemaPromotions.startBundling(entry)) {
            val bundledSchema =
                when (referencedModel) {
                    is Schema ->
                        SchemaInterface.SchemaInline(
                            bundleSchema(
                                schema = referencedModel,
                                context = context.enterExternalSchema(reference),
                            ),
                        )

                    is MultiFormatSchema ->
                        SchemaInterface.MultiFormatSchemaInline(referencedModel)

                    else ->
                        throw IllegalArgumentException(
                            "Schema reference ${reference.ref} resolved to unsupported model",
                        )
                }
            context.schemaPromotions.complete(entry, bundledSchema)
        }

        if (definesPromotedSchema) {
            return checkNotNull(entry.bundledSchema) {
                "Root schema '${entry.name}' was not bundled"
            }
        }

        return SchemaInterface.SchemaReference(
            Reference(
                ref = componentSchemaReference(entry.name),
                model = entry.model,
                referenceCategoryKey = reference.referenceCategoryKey ?: ReferenceCategoryKey.SCHEMA,
            ),
        )
    }

    private fun Any.toInlineSchemaInterface(): SchemaInterface =
        when (this) {
            is Schema -> SchemaInterface.SchemaInline(this)
            is MultiFormatSchema -> SchemaInterface.MultiFormatSchemaInline(this)
            else -> throw IllegalArgumentException("Schema reference resolved to unsupported model")
        }

    private fun bundleSchema(schema: Schema, context: BundlingContext): Schema {
        val bundledItems = schema.items?.let { bundle(it, context) }
        val bundledAdditionalItems = schema.additionalItems?.let { bundle(it, context) }
        val bundledContains = schema.contains?.let { bundle(it, context) }

        val bundledProperties = bundleMap(schema.properties, context)
        val bundledPatternProperties = bundleMap(schema.patternProperties, context)
        val bundledAdditionalProperties = schema.additionalProperties?.let { bundle(it, context) }
        val bundledPropertyNames = schema.propertyNames?.let { bundle(it, context) }
        val bundledDependencies = schema.dependencies?.mapValues { (_, dependency) ->
            if (dependency is SchemaInterface) bundle(dependency, context) else dependency
        }
        val bundledDefinitions = bundleMap(schema.definitions, context)

        val bundledAllOf = bundleList(schema.allOf, context)
        val bundledAnyOf = bundleList(schema.anyOf, context)
        val bundledOneOf = bundleList(schema.oneOf, context)
        val bundledNot = schema.not?.let { bundle(it, context) }
        val bundledIf = schema.ifSchema?.let { bundle(it, context) }
        val bundledThen = schema.thenSchema?.let { bundle(it, context) }
        val bundledElse = schema.elseSchema?.let { bundle(it, context) }

        val bundledExternalDocs = schema.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledBindings = bindingsBundler.bundleMap(schema.bindings, context)
        return schema.copy(
            items = bundledItems,
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

    private fun isComponentSchemaRef(ref: String): Boolean {
        return ref.componentSchemaNameOrNull() != null
    }
}
