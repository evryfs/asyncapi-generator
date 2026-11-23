package dev.banking.asyncapi.generator.core.model.references

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.util.Collections
import java.util.IdentityHashMap

/**
 * A `$ref` and, once resolved, the model it points at.
 *
 * Equality, hash code and string form only use what the reference points at ([ref], [sourceId] and
 * [referenceCategoryKey]), never the resolved [model]: models can refer back to themselves, so
 * following [model] would never end.
 *
 * @param ref the `$ref` text as written
 * @param model the model the reference points at, once linked
 * @param sourceId the unique id of the document the reference is written in
 * @param referenceCategoryKey the model type expected at the reference target
 * @param inline whether the reference is written as its [model] instead of as a `$ref`
 */
data class Reference(
    @get:JsonIgnore
    val ref: String,
    @get:JsonIgnore
    var model: Any? = null,
    @get:JsonIgnore
    var sourceId: String? = null,
    @get:JsonIgnore
    var referenceCategoryKey: ReferenceCategoryKey? = null,
    @get:JsonIgnore
    val inline: Boolean = false,
) {
    /** A copy of this reference that is written as [model] instead of as a `$ref`. */
    fun inlined(model: Any?): Reference = copy(model = model, inline = true)

    @JsonProperty($$"$ref")
    fun getRefForSerialization(): String? = if (inline) null else ref

    @JsonUnwrapped
    fun getModelForSerialization(): Any? = if (inline) model else null

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is Reference &&
                    ref == other.ref &&
                    sourceId == other.sourceId &&
                    referenceCategoryKey == other.referenceCategoryKey
            )

    override fun hashCode(): Int = ref.hashCode()

    override fun toString(): String = "Reference(ref=$ref)"

    /** The model at the end of this reference's chain, or `null` when the chain is not linked or never ends. */
    fun finalModel(): Any? {
        val followed = Collections.newSetFromMap(IdentityHashMap<Reference, Boolean>())
        var current: Any? = this
        while (current is Reference && followed.add(current)) {
            current = current.model
        }
        return current?.takeUnless { it is Reference }
    }

    /** The model at the end of this reference's chain; fails when the reference is not linked to a model. */
    inline fun <reified T> requireModel(): T =
        checkNotNull(finalModel()) {
            "Reference '$ref' is not linked to a model"
        } as T
}
