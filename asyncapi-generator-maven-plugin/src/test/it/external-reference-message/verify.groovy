def output = new File(basedir, "target/generated-sources/asyncapi")
def model = new File(
    output,
    "com/example/contract/model/MyReferencedSchema.kt",
)
def producer = new File(
    output,
    "com/example/contract/client/producer/AuditEventsProducer.kt",
)
def consumer = new File(
    output,
    "com/example/contract/client/consumer/AuditEventsConsumer.kt",
)

assert model.isFile() :
    "Expected the payload referenced by the external Message Object to be generated"
assert model.text.contains("data class MyReferencedSchema(")
assert model.text.contains("val firstField: Int")
assert model.text.contains("val secondField: String?")

assert producer.isFile() : "Expected the producer contract to be generated"
assert producer.text.contains("payload: MyReferencedSchema")

assert consumer.isFile() : "Expected the consumer contract to be generated"
assert consumer.text.contains("payload: MyReferencedSchema")
