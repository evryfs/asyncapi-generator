package dev.banking.asyncapi.generator.core.generator.avro.model

/** An Avro type, written as the JSON that an `.avsc` file holds for it. */
internal sealed interface AvroType {
    /** The JSON of this type. */
    fun json(): String

    /**
     * A primitive type, such as `"long"`.
     *
     * @property name the Avro primitive type name
     */
    data class Primitive(
        val name: String,
    ) : AvroType {
        override fun json(): String = "\"$name\""
    }

    /**
     * A primitive type with a logical type, such as a `date` stored as an `int`.
     *
     * @property base the primitive type that stores the value
     * @property logicalType the Avro logical type name
     */
    data class Logical(
        val base: String,
        val logicalType: String,
    ) : AvroType {
        override fun json(): String = "{\"type\": \"$base\", \"logicalType\": \"$logicalType\"}"
    }

    /**
     * An array.
     *
     * @property items the type of each item
     */
    data class ArrayOf(
        val items: AvroType,
    ) : AvroType {
        override fun json(): String = "{\"type\": \"array\", \"items\": ${items.json()}}"
    }

    /**
     * A map with string keys.
     *
     * @property values the type of each value
     */
    data class MapOf(
        val values: AvroType,
    ) : AvroType {
        override fun json(): String = "{\"type\": \"map\", \"values\": ${values.json()}}"
    }

    /**
     * A record or enum declared in its own `.avsc` file, referred to by its full name.
     *
     * @property fullName the namespace and name, such as `com.example.avro.Account`
     */
    data class Named(
        val fullName: String,
    ) : AvroType {
        override fun json(): String = "\"$fullName\""
    }

    /**
     * A union. Avro unions cannot contain unions, so a union never is a branch of another.
     *
     * @property branches the branch types, in order
     */
    data class Union(
        val branches: List<AvroType>,
    ) : AvroType {
        override fun json(): String = branches.joinToString(", ", "[", "]") { it.json() }
    }

    companion object {
        val NULL = Primitive("null")
        val STRING = Primitive("string")

        /**
         * [type] or null: `null` comes first, unless the field has a default that is not null,
         * because a union's default must match its first branch.
         */
        fun optional(
            type: AvroType,
            hasDefault: Boolean,
        ): Union {
            val branches = if (type is Union) type.branches else listOf(type)
            return Union(if (hasDefault) branches + NULL else listOf(NULL) + branches)
        }
    }
}
