package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Checks the `clientConfig` settings and resolves them into a [ClientGeneration].
 *
 * Client configuration is usually shared by all executions of a build, so the
 * settings are checked on every execution, also on one without `clientPackage`
 * that generates no client.
 */
internal object ClientConfigurationResolver {
    /**
     * Checks the client settings that do not depend on the generator profile, or
     * returns `null` when [settings] have no `clientConfig`.
     */
    fun check(settings: GeneratorSettings): CheckedClientConfig? {
        val clientConfig = settings.clientConfig ?: return null
        ClientType.fromConfigurationValue(clientConfig.clientType, "clientConfig.clientType")
        ClientContract.fromConfigurationValue(clientConfig.clientContract, "clientConfig.clientContract")
        settings.clientPackage?.let { PackageName.fromConfigurationValue(it, "clientPackage") }
        when {
            settings.modelPackage != null -> PackageName.fromConfigurationValue(settings.modelPackage, "modelPackage")
            settings.clientPackage != null ->
                throw IllegalArgumentException("modelPackage is required when clientPackage is configured")
        }
        return CheckedClientConfig(
            clientConfig,
            validationAnnotations(clientConfig.validationAnnotations),
        )
    }

    /** The warning for client settings on an execution without `clientPackage`, which generates no client. */
    fun unusedClientConfigWarning(settings: GeneratorSettings): ConfigurationWarning? =
        if (settings.clientConfig != null && settings.clientPackage == null) {
            ConfigurationWarning(
                id = ConfigurationWarningId.UNUSED_CLIENT_CONFIG,
                message =
                    "clientConfig is ignored because clientPackage is not configured, so no client is generated. " +
                        "Add '${ConfigurationWarningId.UNUSED_CLIENT_CONFIG.configurationValue}' to " +
                        "suppressedWarnings to silence this warning.",
            )
        } else {
            null
        }

    /** The client to generate, or `null` when [settings] configure no `clientPackage`. */
    fun client(
        settings: GeneratorSettings,
        checked: CheckedClientConfig?,
    ): ClientGeneration? {
        val clientPackage = settings.clientPackage ?: return null
        val clientConfig = checkNotNull(checked).clientConfig
        return ClientGeneration(
            packageName = clientPackage,
            modelPackageName = checkNotNull(settings.modelPackage),
            topicParameterProperties =
                TopicParameterProperties.fromConfigurationValues(
                    values = clientConfig.topicParameterProperties,
                    path = "clientConfig.topicParameterProperties",
                ),
            validationAnnotations = checked.validationAnnotations,
            producer =
                ClientGeneration.Producer(
                    enabled = clientConfig.producer?.enabled ?: true,
                    additionalPayloadTypes =
                        AdditionalProducerPayloadType.fromConfigurationValues(
                            values = clientConfig.producer?.additionalPayloadTypes,
                            path = "clientConfig.producer.additionalPayloadTypes",
                        ),
                ),
            consumer = ClientGeneration.Consumer(enabled = clientConfig.consumer?.enabled ?: true),
        )
    }

    private fun validationAnnotations(
        annotations: GeneratorSettings.ValidationAnnotationsConfig?,
    ): ClientValidationAnnotations =
        ClientValidationAnnotations(
            clientContract =
                annotations?.clientContract?.let {
                    QualifiedTypeName.fromConfigurationValue(it, "clientConfig.validationAnnotations.clientContract")
                },
            payloadParameter =
                annotations?.payloadParameter?.let {
                    QualifiedTypeName.fromConfigurationValue(it, "clientConfig.validationAnnotations.payloadParameter")
                },
        )

    /**
     * Client settings that passed the checks made on every execution.
     *
     * @param clientConfig the settings as configured.
     * @param validationAnnotations the resolved validation annotations.
     */
    data class CheckedClientConfig(
        val clientConfig: GeneratorSettings.ClientConfig,
        val validationAnnotations: ClientValidationAnnotations,
    )
}
