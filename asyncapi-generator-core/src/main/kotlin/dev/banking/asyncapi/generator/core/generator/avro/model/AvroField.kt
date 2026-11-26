package dev.banking.asyncapi.generator.core.generator.avro.model

/**
 * A field of an Avro record.
 *
 * @property name the field name, which is the property's wire name
 * @property doc the description as JSON-escaped `doc` text; null without one
 * @property jsonType the Avro type as JSON, such as `["null", "string"]`
 * @property last whether this is the last field, so the template omits the trailing comma
 * @property defaultJson the default as Avro JSON, such as `null` or `"silver"`; null without a default
 */
internal data class AvroField(
    val name: String,
    val doc: String?,
    val jsonType: String,
    val last: Boolean = false,
    val defaultJson: String? = null,
)
