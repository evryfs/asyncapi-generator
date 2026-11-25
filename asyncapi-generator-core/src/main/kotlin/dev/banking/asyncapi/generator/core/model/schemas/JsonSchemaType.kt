package dev.banking.asyncapi.generator.core.model.schemas

import com.fasterxml.jackson.annotation.JsonValue

/**
 * The `type` keyword of a Schema Object: one type name, or a list of type names. It is written back
 * in the form it was read.
 */
sealed interface JsonSchemaType {
    /** The type names, in the order the schema lists them. */
    val names: List<String>

    /** The main type: the single name, or the first name of a list other than `null`. */
    val primary: String?

    /** Whether the schema allows `null`. */
    val allowsNull: Boolean
        get() = NULL in names

    /** Whether the schema names more than one distinct type other than `null`. */
    val hasMultipleNonNullTypes: Boolean
        get() = names.filter { it != NULL }.distinct().size > 1

    /**
     * A type written as a single name, such as `type: string`.
     *
     * @property name the type name
     */
    data class Single(
        @get:JsonValue val name: String,
    ) : JsonSchemaType {
        override val names: List<String>
            get() = listOf(name)

        override val primary: String
            get() = name

        override fun toString(): String = name
    }

    /**
     * A type written as a list of names, such as `type: [string, "null"]`.
     *
     * @property names the type names, in the order the schema lists them
     */
    data class Multiple(
        @get:JsonValue override val names: List<String>,
    ) : JsonSchemaType {
        override val primary: String?
            get() = names.firstOrNull { it != NULL }

        override fun toString(): String = names.toString()
    }

    private companion object {
        const val NULL = "null"
    }
}
