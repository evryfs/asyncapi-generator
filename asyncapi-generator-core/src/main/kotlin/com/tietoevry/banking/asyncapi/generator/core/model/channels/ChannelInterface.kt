package com.tietoevry.banking.asyncapi.generator.core.model.channels

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface ChannelInterface {

    data class ChannelInline(
        @get:JsonUnwrapped
        val channel: Channel,
    ) : ChannelInterface

    data class ChannelReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ChannelInterface
}
