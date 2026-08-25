package dev.banking.asyncapi.generator.core.generator.analyzer

data class AnalyzedChannel(
    val channelName: String,
    val topic: String?,
    val messages: List<AnalyzedMessage>,
    val multiFormatMessages: List<AnalyzedMultiFormatMessage> = emptyList(),
)
