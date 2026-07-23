def producer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/producer/UserEventsProducer.kt")
assert producer.exists() : "Expected UserEventsProducer.kt to be generated"

def producerContent = producer.text
assert producerContent.contains("interface UserEventsProducer {") : "Expected channel-level producer contract interface"
assert producerContent.contains("@Validated") : "Expected configured client contract validation annotation"
assert producerContent.contains("@Valid") : "Expected configured payload parameter validation annotation"
assert producerContent.contains("CompletableFuture<RecordMetadata>") : "Expected producer acknowledgement metadata return type"
assert producerContent.contains('const val USER_EVENTS_TOPIC_ADDRESS: String = "user.\\${kafka.environment}.events.v1"') : "Expected mapped producer topic address"
assert !producerContent.contains("resolveTopicAddress") : "Producer contract should not expose runtime topic resolution"
assert !producerContent.contains("@Component") : "Contract producer should not be annotated"

def consumer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/consumer/UserEventsConsumer.kt")
assert consumer.exists() : "Expected UserEventsConsumer.kt to be generated"

def consumerContent = consumer.text
assert consumerContent.contains("interface UserEventsConsumer") : "Expected consumer interface"
assert consumerContent.contains("@Validated") : "Expected configured client contract validation annotation"
assert consumerContent.contains("@Valid") : "Expected configured payload parameter validation annotation"
assert consumerContent.contains("fun listen(") : "Expected single-message listen method"
assert consumerContent.contains('const val USER_EVENTS_TOPIC_ADDRESS: String = "user.\\${kafka.environment}.events.v1"') : "Expected mapped consumer topic address"
assert !consumerContent.contains("resolveTopicAddress") : "Consumer contract should not expose runtime topic resolution"
assert !consumerContent.contains("{ }") : "Expected abstract consumer method"
assert !consumerContent.contains("@KafkaListener") : "Contract consumer should not be annotated"
