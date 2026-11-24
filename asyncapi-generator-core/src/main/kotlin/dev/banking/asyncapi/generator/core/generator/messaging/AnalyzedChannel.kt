package dev.banking.asyncapi.generator.core.generator.messaging

/**
 * A channel of the document with the messages it carries.
 *
 * Messages with a payload in a native schema format follow the others, each group in document order.
 *
 * @param channelName Key of the channel in the document.
 * @param topic Address of the channel, or `null` when it has none.
 * @param messages Messages of the channel that client generation can use.
 */
internal data class AnalyzedChannel(
    val channelName: String,
    val topic: String?,
    val messages: List<AnalyzedMessage>,
)
