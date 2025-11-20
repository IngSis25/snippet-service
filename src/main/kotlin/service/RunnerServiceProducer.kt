package service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.SnippetMessage
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

interface RunnerServiceProducer {
    suspend fun publishEvent(snippetMessage: SnippetMessage)
}

@Service
@Profile("!test")
class RedisRunnerServiceProducer(
    @Value("\${stream.runner.key}") streamKey: String,
    redis: ReactiveRedisTemplate<String, String>
) : RunnerServiceProducer, RedisStreamProducer(streamKey, redis) {
    
    override suspend fun publishEvent(snippetMessage: SnippetMessage) {
        println("Publicando evento al runner-service")
        val messageJson = jacksonObjectMapper().writeValueAsString(snippetMessage)
        println("Mensaje a publicar: $messageJson")
        emit(messageJson).awaitSingle()
        println("Evento publicado exitosamente")
    }
}

