def output = new File(basedir, "target/generated-sources/asyncapi")
def modelPackage = new File(output, "com/example/references/model")
def producerPackage = new File(output, "com/example/references/client/producer")
def consumerPackage = new File(output, "com/example/references/client/consumer")

def assertModel = { String name, String expectedProperty ->
    def model = new File(modelPackage, "${name}.kt")
    assert model.isFile() : "Expected ${name}.kt to be generated"
    assert model.text.contains(expectedProperty) :
        "Expected ${name}.kt to contain '${expectedProperty}'"
}

def assertContracts = { String channelName, String methodSuffix, String payloadType ->
    def producer = new File(producerPackage, "${channelName}Producer.kt")
    def consumer = new File(consumerPackage, "${channelName}Consumer.kt")

    assert producer.isFile() : "Expected ${channelName}Producer.kt to be generated"
    assert producer.text.contains("fun send${methodSuffix}(")
    assert producer.text.contains("payload: ${payloadType}")

    assert consumer.isFile() : "Expected ${channelName}Consumer.kt to be generated"
    assert consumer.text.contains("fun listen${methodSuffix}(")
    assert consumer.text.contains("payload: ${payloadType}")
}

// A raw external Message Object map without an AsyncAPI document wrapper.
assertModel("RawMessageV1Payload", "val rawValue: String")
assertContracts("RawMessageEvents", "RawMessageV1", "RawMessageV1Payload")

// A channel message whose payload points directly to an external Schema Object.
assertModel("DirectPayloadV1Payload", "val directValue: Int")
assertContracts("DirectPayloadEvents", "DirectPayloadV1", "DirectPayloadV1Payload")

// An external Channel Object with owner-relative references to its message and payload.
assertModel("ExternalChannelMessageV1Payload", "val externalValue: Boolean")
assertContracts(
    "ExternalChannelEvents",
    "ExternalChannelMessageV1",
    "ExternalChannelMessageV1Payload",
)

// Raw external schema fragments are materialized as message-owned payload models.
assertModel(
    "SharedPayloadCreatedV1Payload",
    "val sharedValue: String",
)
assertContracts(
    "SharedPayloadEvents",
    "SharedPayloadCreatedV1",
    "SharedPayloadCreatedV1Payload",
)
assertModel(
    "SharedPayloadUpdatedV1Payload",
    "val sharedValue: String",
)
assertContracts(
    "SharedPayloadEvents",
    "SharedPayloadUpdatedV1",
    "SharedPayloadUpdatedV1Payload",
)
assert !new File(modelPackage, "SharedPayload.kt").exists() :
    "Did not expect a shared component model for raw external schema fragments"

// A recursive external payload must retain its self-reference and compile.
assertModel("RecursiveNodePayload", "val children: List<RecursiveNodePayload>? = null")
assertContracts("RecursiveNodeEvents", "RecursiveNodeV1", "RecursiveNodePayload")

def classes = new File(basedir, "target/classes")
[
    "RawMessageV1Payload",
    "DirectPayloadV1Payload",
    "ExternalChannelMessageV1Payload",
    "SharedPayloadCreatedV1Payload",
    "SharedPayloadUpdatedV1Payload",
    "RecursiveNodePayload",
].each { modelName ->
    assert new File(
        classes,
        "com/example/references/model/${modelName}.class",
    ).isFile() : "Expected ${modelName} to compile"
}

[
    "RawMessageEvents",
    "DirectPayloadEvents",
    "ExternalChannelEvents",
    "SharedPayloadEvents",
    "RecursiveNodeEvents",
].each { channelName ->
    assert new File(
        classes,
        "com/example/references/client/producer/${channelName}Producer.class",
    ).isFile() : "Expected ${channelName}Producer to compile"
    assert new File(
        classes,
        "com/example/references/client/consumer/${channelName}Consumer.class",
    ).isFile() : "Expected ${channelName}Consumer to compile"
}

true
