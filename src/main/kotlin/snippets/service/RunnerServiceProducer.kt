package snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.stream.MapRecord
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
    @Value("\${stream.runner.key}") private val streamKey: String,
) : RunnerServiceProducer {
    override fun publishSnippetEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento al runner-service")
        val messageJson = jacksonObjectMapper().writeValueAsString(snippetMessage)
        println("Mensaje a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "snippet")
        val record: MapRecord<String, String, String> = MapRecord.create(streamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento publicado exitosamente")
    }

    override fun publishTestEvent(testMessage: TestMessage) {
        println("Publicando evento de test al runner-service")
        val messageJson = jacksonObjectMapper().writeValueAsString(testMessage)
        println("Mensaje de test a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "test")
        val record: MapRecord<String, String, String> = MapRecord.create(streamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de test publicado exitosamente")
    }

    override fun publishFormatEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento de format al runner-service")
        val messageJson = jacksonObjectMapper().writeValueAsString(snippetMessage)
        println("Mensaje de format a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "format")
        val record: MapRecord<String, String, String> = MapRecord.create(streamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de format publicado exitosamente")
    }

    override fun publishLintEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento de lint al runner-service")
        val messageJson = jacksonObjectMapper().writeValueAsString(snippetMessage)
        println("Mensaje de lint a publicar: $messageJson")

        val message = mapOf("data" to messageJson, "type" to "lint")
        val record: MapRecord<String, String, String> = MapRecord.create(streamKey, message)
        redisTemplate.opsForStream<String, String>().add(record)

        println("Evento de lint publicado exitosamente")
    }
}
