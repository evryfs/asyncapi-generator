package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Value type exposed by generated Spring Kafka producer send results.
 *
 * Expected behavior is covered by:
 * - `ProducerRecordValueTypeTest`
 */
sealed interface ProducerRecordValueType {
    /** Uses `byte[]` for Java output and `ByteArray` for Kotlin output. */
    data object ByteArray : ProducerRecordValueType

    /** Uses an explicitly configured source type. */
    data class Custom(
        val typeName: QualifiedTypeName,
    ) : ProducerRecordValueType

    companion object {
        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ProducerRecordValueType =
            value?.let {
                Custom(
                    QualifiedTypeName.fromConfigurationValue(
                        value = it,
                        path = path,
                    ),
                )
            } ?: ByteArray
    }
}
