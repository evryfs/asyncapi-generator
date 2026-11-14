package dev.banking.asyncapi.generator.core.generator.avro.model

data class AvroField(
    val name: String,
    val doc: String?,
    val jsonType: String, // Pre-calculated JSON structure (e.g. ["null", "string"])
    val last: Boolean = false, // Helper for comma handling in JSON templates
    /** Default value as Avro JSON, such as `null` or `"silver"`; null when the field has no default. */
    val defaultJson: String? = null,
)
