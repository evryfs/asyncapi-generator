def producer = new File(basedir, "target/generated-sources/asyncapi/com/example/codegen/client/SampleProducer.kt")

assert producer.exists() : "Expected SampleProducer.kt to be generated"

def text = producer.text
assert text.contains('@Value("\\${my.property.sample}")') : "Expected property key to be my.property.sample"
