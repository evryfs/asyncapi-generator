package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiBundlingException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.componentSchemaNameOrNull
import dev.banking.asyncapi.generator.core.model.references.referenceTargetNameOrNull
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.IdentityHashMap

/** Stores recursive external schemas promoted into the bundled root document. */
internal class PromotedSchemaRegistry(
    rootSchemas: Map<String, SchemaInterface> = emptyMap(),
) {

    internal class Entry(
        val name: String,
        val model: Any,
        val origin: String,
        val promoted: Boolean,
        var state: State,
        var bundledSchema: SchemaInterface? = null,
    )

    internal enum class State {
        PENDING,
        BUNDLING,
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

    fun reserve(reference: Reference, model: Any): Entry {
        entriesByModel[model]?.let { return it }

        val name = reference.ref.componentSchemaNameOrNull()
            ?: reference.ref.referenceTargetNameOrNull()
            ?: throw AsyncApiBundlingException.ExternalSchemaNameUnavailable(reference.ref)
        val origin = reference.origin()
        entriesByName[name]?.let { existing ->
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

    fun startBundling(entry: Entry): Boolean {
        if (entry.state != State.PENDING) return false
        entry.state = State.BUNDLING
        return true
    }

    fun complete(entry: Entry, schema: SchemaInterface) {
        entry.bundledSchema = schema
        entry.state = State.COMPLETE
    }

    fun schemas(): Map<String, SchemaInterface> =
        entriesByName.values
            .filter(Entry::promoted)
            .associateTo(linkedMapOf()) { entry ->
                entry.name to checkNotNull(entry.bundledSchema) {
                    "Promoted schema '${entry.name}' was not bundled"
                }
            }

    private fun SchemaInterface.modelIdentity(): Any =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.requireModel()
            is SchemaInterface.MultiFormatSchemaInline -> multiFormatSchema
            is SchemaInterface.BooleanSchema -> this
        }

    private fun SchemaInterface.isExternalReference(): Boolean =
        this is SchemaInterface.SchemaReference && reference.ref.substringBefore('#').isNotBlank()

    private fun Reference.origin(): String =
        sourceId?.let { source -> "$source:$ref" } ?: ref
}
