def producer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/producer/UserEventsProducerUserSignedUp.kt")
assert producer.exists() : "Expected UserEventsProducerUserSignedUp.kt to be generated"

def producerContent = producer.text
assert producerContent.contains("class UserEventsProducerUserSignedUp") : "Expected producer class name"
assert producerContent.contains("KafkaTemplate<String, UserSignedUp>") : "Expected typed KafkaTemplate"
assert producerContent.contains("CompletableFuture<SendResult<String, UserSignedUp>>") : "Expected producer send future return type"
assert !producerContent.contains("@Component") : "Contract producer should not be annotated"

def consumer = new File(basedir, "target/generated-sources/asyncapi/com/example/contract/client/consumer/UserEventsConsumer.kt")
assert consumer.exists() : "Expected UserEventsConsumer.kt to be generated"

def consumerContent = consumer.text
assert consumerContent.contains("interface UserEventsConsumer") : "Expected consumer interface"
assert consumerContent.contains("fun onUserSignedUp") : "Expected onUserSignedUp method"
assert !consumerContent.contains("{ }") : "Expected abstract consumer method"
assert !consumerContent.contains("@KafkaListener") : "Contract consumer should not be annotated"
