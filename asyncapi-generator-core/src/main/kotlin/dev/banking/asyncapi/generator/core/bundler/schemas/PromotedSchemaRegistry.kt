package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundlingException
import dev.banking.asyncapi.generator.core.bundler.AsyncApiDocumentSerializer
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.componentSchemaNameOrNull
import dev.banking.asyncapi.generator.core.model.references.referenceTargetNameOrNull
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.IdentityHashMap

/**
 * Stores external schemas promoted into the bundled root document under their component name.
 *
 * Contracts often copy shared types into several files. An external schema whose name is already
 * taken by a schema with the same content reuses that schema; one with different content fails,
 * because one name cannot describe two types.
 *
 * @param rootSchemas the component schemas of the root document, which hold their names from the start
 */
internal class PromotedSchemaRegistry(
    rootSchemas: Map<String, SchemaInterface> = emptyMap(),
) {
    /**
     * A schema name in the bundled `components.schemas` and the schema that holds it.
     *
     * @param name the component schema name
     * @param model the source model, which identifies the schema
     * @param origin where the schema comes from, shown when two schemas collide
     * @param promoted whether the schema is promoted from outside the root components
     * @param state how far bundling of the schema has come
     * @param bundledSchema the bundled schema, once complete
     */
    internal class Entry(
        val name: String,
        val model: Any,
        val origin: String,
        val promoted: Boolean,
        var state: State,
        var bundledSchema: SchemaInterface? = null,
    )

    /** How far bundling of a schema has come; a schema reached again while [BUNDLING] is recursive. */
    internal enum class State {
        /** Reserved, not bundled yet. */
        PENDING,

        /** Being bundled. */
        BUNDLING,

        /** Bundled; [Entry.bundledSchema] holds the result. */
        COMPLETE,
    }

    private val entriesByModel = IdentityHashMap<Any, Entry>()
    private val entriesByName = linkedMapOf<String, Entry>()

    init {
        rootSchemas.forEach { (name, schema) ->
            val model = schema.modelIdentity()
            val entry =
                Entry(
                    name = name,
                    model = model,
                    origin = "#/components/schemas/$name",
                    promoted = false,
                    state = if (schema.isExternalReference()) State.PENDING else State.COMPLETE,
                    bundledSchema = schema.takeUnless { it.isExternalReference() },
                )
            entriesByName[name] = entry
            entriesByModel.putIfAbsent(model, entry)
        }
    }

    /**
     * Returns the entry for [model]: an existing one for the same model or for the same name and content,
     * or a new one under the name of [reference]'s pointer.
     *
     * @throws AsyncApiBundlingException when the pointer gives no name, or the name holds a different schema
     */
    fun reserve(
        reference: Reference,
        model: Any,
    ): Entry {
        entriesByModel[model]?.let { return it }

        val name =
            reference.ref.componentSchemaNameOrNull()
                ?: reference.ref.referenceTargetNameOrNull()
                ?: throw AsyncApiBundlingException.ExternalSchemaNameUnavailable(reference.ref)
        val origin = reference.origin()
        entriesByName[name]?.let { existing ->
            if (sameContent(existing.model, model)) {
                entriesByModel[model] = existing
                return existing
            }
            throw AsyncApiBundlingException.PromotedSchemaNameCollision(
                schemaName = name,
                existingOrigin = existing.origin,
                incomingOrigin = origin,
            )
        }

        return Entry(
            name = name,
            model = model,
            origin = origin,
            promoted = true,
            state = State.PENDING,
        ).also { entry ->
            entriesByModel[model] = entry
            entriesByName[name] = entry
        }
    }

    /** Marks [entry] as being bundled; `false` when it is already being bundled or complete. */
    fun startBundling(entry: Entry): Boolean {
        if (entry.state != State.PENDING) return false
        entry.state = State.BUNDLING
        return true
    }

    /** Stores the bundled [schema] of [entry]. */
    fun complete(
        entry: Entry,
        schema: SchemaInterface,
    ) {
        entry.bundledSchema = schema
        entry.state = State.COMPLETE
    }

    /** The promoted schemas by name, in the order they were reserved. */
    fun schemas(): Map<String, SchemaInterface> =
        entriesByName.values
            .filter(Entry::promoted)
            .associateTo(linkedMapOf()) { entry ->
                entry.name to
                    checkNotNull(entry.bundledSchema) {
                        "Promoted schema '${entry.name}' was not bundled"
                    }
            }

    /** Compares the schemas as written, with nested references as their `$ref` text. */
    private fun sameContent(
        first: Any,
        second: Any,
    ): Boolean =
        first === second || AsyncApiDocumentSerializer.toJson(first) == AsyncApiDocumentSerializer.toJson(second)

    private fun SchemaInterface.modelIdentity(): Any =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.requireModel()
            is SchemaInterface.MultiFormatSchemaInline -> multiFormatSchema
            is SchemaInterface.BooleanSchema -> this
        }

    private fun SchemaInterface.isExternalReference(): Boolean =
        this is SchemaInterface.SchemaReference && reference.ref.substringBefore('#').isNotBlank()

    private fun Reference.origin(): String = sourceId?.let { source -> "$source:$ref" } ?: ref
}
