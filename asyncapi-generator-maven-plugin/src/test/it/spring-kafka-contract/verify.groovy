def producer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/producer/UserEventsProducer.kt")
assert producer.exists() : "Expected UserEventsProducer.kt to be generated"

def producerContent = producer.text
assert producerContent.contains("interface UserEventsProducer {") : "Expected channel-level producer contract interface"
assert producerContent.contains("@Validated") : "Expected configured client contract validation annotation"
assert producerContent.contains("@Valid") : "Expected configured payload parameter validation annotation"
assert producerContent.contains("fun sendUserSignedUp(") : "Expected message-qualified send method"
assert producerContent.contains("fun sendUserSignedUpByteArray(") : "Expected additive byte-array send method"
assert producerContent.contains("fun sendUserSignedUpString(") : "Expected additive string send method"
assert producerContent.contains("CompletableFuture<RecordMetadata>") : "Expected producer acknowledgement metadata return type"
assert producerContent.contains("CompletableFuture.failedFuture(") : "Expected an explicit unimplemented producer default"
assert producerContent.contains('const val USER_EVENTS_TOPIC_ADDRESS: String = "user.\\${kafka.environment}.events.v1"') : "Expected mapped producer topic address"
assert !producerContent.contains("resolveTopicAddress") : "Producer contract should not expose runtime topic resolution"
assert !producerContent.readLines().any { line -> line.trim() == "@Component" } :
    "Contract producer should not be annotated"

def consumer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/consumer/UserEventsConsumer.kt")
assert consumer.exists() : "Expected UserEventsConsumer.kt to be generated"

def consumerContent = consumer.text
assert consumerContent.contains("interface UserEventsConsumer") : "Expected consumer interface"
assert consumerContent.contains("@Validated") : "Expected configured client contract validation annotation"
assert consumerContent.contains("@Valid") : "Expected configured payload parameter validation annotation"
assert consumerContent.contains("fun listenUserSignedUp(") : "Expected message-qualified listen method"
assert consumerContent.contains('const val USER_EVENTS_TOPIC_ADDRESS: String = "user.\\${kafka.environment}.events.v1"') : "Expected mapped consumer topic address"
assert !consumerContent.contains("resolveTopicAddress") : "Consumer contract should not expose runtime topic resolution"
assert consumerContent.contains(") = Unit") : "Expected a no-op consumer default"
assert !consumerContent.readLines().any { line -> line.trim().startsWith("@KafkaListener") } : "Contract consumer should not register a listener"
assert !consumerContent.readLines().any { line -> line.trim() == "@KafkaHandler" } : "Contract consumer should not select Kafka handlers"

def auditProducer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/producer/AuditEventsProducer.kt")
assert auditProducer.exists() : "Expected a producer for the receive-only auditEvents channel"
assert auditProducer.text.contains("fun sendAuditRecorded(") :
    "Expected the receive operation not to suppress producer generation"

def auditConsumer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/consumer/AuditEventsConsumer.kt")
assert auditConsumer.exists() : "Expected a consumer for the receive-only auditEvents channel"
assert auditConsumer.text.contains("fun listenAuditRecorded(") :
    "Expected the receive-only channel message in the consumer contract"

true
