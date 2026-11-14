package dev.banking.asyncapi.generator.core.model.references

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

/**
 * A `$ref` and, once resolved, the model it points at.
 *
 * Equality, hash code and string form only use what the reference points at ([ref], [sourceId] and
 * [referenceCategoryKey]), never the resolved [model]: models can refer back to themselves, so
 * following [model] would never end.
 */
data class Reference(
    @get:JsonIgnore
    val ref: String, // e.g. "#/channels/myChannel"
    @get:JsonIgnore
    var model: Any? = null, // The actual model being referenced (if resolved)
    @get:JsonIgnore
    var sourceId: String? = null,
    @get:JsonIgnore
    var referenceCategoryKey: ReferenceCategoryKey? = null, // The model type expected at the reference target.
) {

    @get:JsonIgnore
    var inline: Boolean = false // only show 'ref' by default

    fun inline(): Reference = apply { inline = true }

    @JsonProperty($$"$ref")
    fun getRefForSerialization(): String? = if (inline) null else ref

    @JsonUnwrapped
    fun getModelForSerialization(): Any? = if (inline) model else null

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Reference &&
            ref == other.ref &&
            sourceId == other.sourceId &&
            referenceCategoryKey == other.referenceCategoryKey

    override fun hashCode(): Int = ref.hashCode()

    override fun toString(): String = "Reference(ref=$ref)"

    inline fun <reified T> requireModel(): T {
        var current: Any? = this.model
        while (current is Reference) {
            current = current.model
        }
        return current as T
    }
}
