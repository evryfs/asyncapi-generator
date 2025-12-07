package dev.banking.asyncapi.generator.core.generator.analyzer

data class AnalyzedChannel(
    val channelName: String,
    val topic: String,
    val isProducer: Boolean, // Generate Producer class?
    val isConsumer: Boolean, // Generate Listener/Handler?
    val messages: List<AnalyzedMessage>
)
