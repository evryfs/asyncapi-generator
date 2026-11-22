package dev.banking.asyncapi.generator.core.validator.schemas

/**
 * A schema being checked against the value at one instance path, compared by schema identity.
 *
 * @param schema the schema
 * @param path where the value sits in the instance
 */
internal class InstanceVisit(
    private val schema: Any,
    private val path: String,
) {
    override fun equals(other: Any?): Boolean = other is InstanceVisit && schema === other.schema && path == other.path

    override fun hashCode(): Int = 31 * System.identityHashCode(schema) + path.hashCode()
}
