package dev.banking.asyncapi.generator.core.generator.avro.model

internal data class AvroField(
    val name: String,
    val doc: String?,
    /** Avro type as JSON, such as `["null", "string"]`. */
    val jsonType: String,
    /** Whether this is the last field, so the JSON template can omit the trailing comma. */
    val last: Boolean = false,
    /** Default value as Avro JSON, such as `null` or `"silver"`; null when the field has no default. */
    val defaultJson: String? = null,
)
