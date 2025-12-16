package snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import snippets.config.SnippetMessage
import snippets.config.TestMessage

interface RunnerServiceProducer {
    fun publishSnippetEvent(snippetMessage: SnippetMessage)

    fun publishTestEvent(testMessage: TestMessage)

    fun publishFormatEvent(snippetMessage: SnippetMessage)

    fun publishLintEvent(snippetMessage: SnippetMessage)
}

@Service
class RedisRunnerServiceProducer(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${stream.runner.key}") private val runnerStreamKey: String,
    @Value("\${stream.test.key}") private val testStreamKey: String,
) : RunnerServiceProducer {
    private val objectMapper = jacksonObjectMapper()

    // ================== SNIPPET / FORMAT / LINT (runnerStream) ==================

    override fun publishSnippetEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento al runner-service (snippet)")
        val messageJson = objectMapper.writeValueAsString(snippetMessage)
        println("Mensaje a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "snippet")
        val record: MapRecord<String, String, String> = MapRecord.create(runnerStreamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de snippet publicado exitosamente")
    }

    override fun publishFormatEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento de format al runner-service")
        val messageJson = objectMapper.writeValueAsString(snippetMessage)
        println("Mensaje de format a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "format")
        val record: MapRecord<String, String, String> = MapRecord.create(runnerStreamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de format publicado exitosamente")
    }

    override fun publishLintEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento de lint al runner-service")
        val messageJson = objectMapper.writeValueAsString(snippetMessage)
        println("Mensaje de lint a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "lint")
        val record: MapRecord<String, String, String> = MapRecord.create(runnerStreamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de lint publicado exitosamente")
    }

    // ======================== TESTS (testStream) ========================

    override fun publishTestEvent(testMessage: TestMessage) {
        println("Publicando evento de test al runner-service (testStream)")
        val messageJson = objectMapper.writeValueAsString(testMessage)
        println("Mensaje de test a publicar: $messageJson")

        // En testStream queremos mandar el JSON plano, porque TestExecutionConsumer
        // hace: objectMapper.readValue(record.value, IncomingTestMessage::class.java)
        val record: ObjectRecord<String, String> =
            StreamRecords.objectBacked<String, String>(messageJson)
                .withStreamKey(testStreamKey)

        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de test publicado exitosamente en stream: $testStreamKey")
    }
}
