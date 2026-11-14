package dev.banking.asyncapi.generator.core.generator.ir

/**
 * The type of a generated property, independent of the source language it is rendered in.
 */
sealed interface TypeRef {
    /** A built-in value type. */
    enum class Scalar : TypeRef {
        STRING,
        UUID,
        DATE,
        TIME,
        DATE_TIME,
        INT32,
        INT64,
        FLOAT,
        DOUBLE,
        DECIMAL,
        BOOLEAN,
        ANY,
    }

    data class ListOf(
        val element: TypeRef,
    ) : TypeRef

    /** A map with string keys. */
    data class MapOf(
        val value: TypeRef,
    ) : TypeRef

    /**
     * A generated model type.
     *
     * @property isModel Whether the type is an object or union model whose constraints a
     *   validator should cascade into; unknown names are assumed to be models.
     * @property isOpenObject Whether the reference names an object without properties, which
     *   Kotlin aliases to `Any` and Java does not generate.
     */
    data class Named(
        val name: String,
        val isModel: Boolean,
        val isOpenObject: Boolean = false,
    ) : TypeRef
}

/**
 * Whether a property of this type is annotated for cascaded validation.
 *
 * A list cascades into its element, a map into its value unless that value nests another map.
 * A collection nested in a list or map counts as a model.
 */
val TypeRef.cascadesValidation: Boolean
    get() =
        when (this) {
            is TypeRef.ListOf -> element.holdsModel()
            is TypeRef.MapOf -> !value.containsMap() && value.holdsModel()
            else -> holdsModel()
        }

private fun TypeRef.holdsModel(): Boolean =
    when (this) {
        is TypeRef.Scalar -> false
        is TypeRef.Named -> isModel
        is TypeRef.ListOf, is TypeRef.MapOf -> true
    }

private fun TypeRef.containsMap(): Boolean =
    when (this) {
        is TypeRef.MapOf -> true
        is TypeRef.ListOf -> element.containsMap()
        else -> false
    }
