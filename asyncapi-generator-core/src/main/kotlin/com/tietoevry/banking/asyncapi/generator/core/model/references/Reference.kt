package com.tietoevry.banking.asyncapi.generator.core.model.references

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class Reference(
    @get:JsonIgnore
    val ref: String, // e.g. "#/channels/myChannel"
    @get:JsonIgnore
    var model: Any? = null, // The actual model being referenced (if resolved)
    @get:JsonIgnore
    var sourceId: String? = null,
) {

    @get:JsonIgnore
    var inline: Boolean = false // only show 'ref' by default

    fun inline(): Reference = apply { inline = true }

    @JsonProperty($$"$ref")
    fun getRefForSerialization(): String? = if (inline) null else ref

    @JsonUnwrapped
    fun getModelForSerialization(): Any? = if (inline) model else null

    inline fun <reified T> requireModel(): T {
        var current: Any? = this.model
        while (current is Reference) {
            current = current.model
        }
        return current as T
    }
}
