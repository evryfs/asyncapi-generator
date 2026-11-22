package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.componentSchemaNameOrNull
import dev.banking.asyncapi.generator.core.model.references.componentSchemaReference
import dev.banking.asyncapi.generator.core.model.references.isExternalReference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Decides what a schema reference becomes in the bundle, in this order:
 *
 * 1. A reference to a boolean schema becomes that boolean schema.
 * 2. A reference that leaves the root document and reaches a component schema or a recursive schema is
 *    promoted to `components.schemas` and points there.
 * 3. A reference already followed on this path stays as it is: a `$ref` to a root component schema, or
 *    an inline copy of the schema, unbundled, which ends recursion.
 * 4. A reference to a component schema of the root document stays a `$ref`; the component itself is
 *    bundled with the root components.
 * 5. Any other reference is inlined.
 *
 * @param schemas bundles the members of the schemas a reference reaches
 */
internal class SchemaReferenceBundler(
    private val schemas: SchemaBundler,
) {
    /** Bundles a schema reference by the rules above. */
    fun bundle(
        node: SchemaInterface.SchemaReference,
        scope: BundlingScope,
    ): SchemaInterface {
        val reference = node.reference
        val model = reference.requireModel<Any>()
        val toComponentSchema = reference.ref.componentSchemaNameOrNull() != null
        return when {
            model is SchemaInterface.BooleanSchema -> model
            leavesRootDocument(reference, scope) &&
                (toComponentSchema || scope.run.schemaRecursion.isRecursive(model)) -> promote(reference, model, scope)
            scope.hasVisited(reference) -> if (toComponentSchema) node else model.toInlineSchema(reference)
            toComponentSchema -> keepReference(node, model, scope)
            else -> inline(reference, model, scope)
        }
    }

    /** Bundles a binding's schema field: any reference that leaves the root document is promoted. */
    fun bundleBindingField(
        node: SchemaInterface.SchemaReference,
        scope: BundlingScope,
    ): SchemaInterface {
        val reference = node.reference
        val model = reference.requireModel<Any>()
        return if (model !is SchemaInterface.BooleanSchema && leavesRootDocument(reference, scope)) {
            promote(reference, model, scope)
        } else {
            bundle(node, scope)
        }
    }

    private fun leavesRootDocument(
        reference: Reference,
        scope: BundlingScope,
    ): Boolean = reference.ref.isExternalReference() || scope.externalDocumentScope

    private fun keepReference(
        node: SchemaInterface.SchemaReference,
        model: Any,
        scope: BundlingScope,
    ): SchemaInterface =
        when (model) {
            is Schema ->
                node.copy(
                    reference = node.reference.copy(model = schemas.bundleMembers(model, scope.enter(node.reference))),
                )
            is MultiFormatSchema -> node
            else -> unsupportedModel(node.reference)
        }

    private fun inline(
        reference: Reference,
        model: Any,
        scope: BundlingScope,
    ): SchemaInterface =
        if (model is Schema) {
            val inner =
                if (reference.ref.isExternalReference()) {
                    scope.enterExternalSchema(
                        reference,
                    )
                } else {
                    scope.enter(reference)
                }
            SchemaInterface.SchemaInline(schemas.bundleMembers(model, inner))
        } else {
            model.toInlineSchema(reference)
        }

    /**
     * Promotes the schema [reference] reaches to `components.schemas`, bundling it the first time it is
     * reached. Inside the definition of the root schema of the same name, the bundled schema itself is
     * returned; anywhere else a `$ref` to the promoted schema.
     */
    private fun promote(
        reference: Reference,
        model: Any,
        scope: BundlingScope,
    ): SchemaInterface {
        val promotions = scope.run.schemaPromotions
        val entry = promotions.reserve(reference, model)
        if (promotions.startBundling(entry)) {
            val bundled =
                if (model is Schema) {
                    SchemaInterface.SchemaInline(schemas.bundleMembers(model, scope.enterExternalSchema(reference)))
                } else {
                    model.toInlineSchema(reference)
                }
            promotions.complete(entry, bundled)
        }
        if (scope.definesRootSchema(entry.name)) {
            return checkNotNull(entry.bundledSchema) { "Root schema '${entry.name}' was not bundled" }
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

    private fun Any.toInlineSchema(reference: Reference): SchemaInterface =
        when (this) {
            is Schema -> SchemaInterface.SchemaInline(this)
            is MultiFormatSchema -> SchemaInterface.MultiFormatSchemaInline(this)
            else -> unsupportedModel(reference)
        }

    private fun unsupportedModel(reference: Reference): Nothing =
        throw IllegalArgumentException("Schema reference ${reference.ref} resolved to unsupported model")
}
