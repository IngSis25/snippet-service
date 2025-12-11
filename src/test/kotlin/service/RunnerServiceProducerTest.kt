package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import snippets.config.SnippetMessage
import snippets.config.TestMessage
import snippets.service.RedisRunnerServiceProducer

@ExtendWith(MockitoExtension::class)
class RunnerServiceProducerTest {
    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var streamOperations: StreamOperations<String, String, String>

    private val runnerStreamKey = "runnerStream"
    private val testStreamKey = "testStream"

    @BeforeEach
    fun setUp() {
        whenever(redisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
    }

    @Test
    fun `publishSnippetEvent should publish message to redis stream`() {
        // Given
        val snippetMessage =
            SnippetMessage(
                snippetId = 1L,
                userId = "auth0|123",
                version = "1.0",
                jwtToken = "test-token",
            )
        val service = RedisRunnerServiceProducer(redisTemplate, runnerStreamKey, testStreamKey)

        // When
        service.publishSnippetEvent(snippetMessage)

        // Then
        verify(redisTemplate).opsForStream<String, String>()
        verify(streamOperations).add(any<MapRecord<String, String, String>>())
    }

    @Test
    fun `publishTestEvent should publish test message to redis stream`() {
        // Given
        val testMessage =
            TestMessage(
                testId = 1L,
                snippetId = 1L,
                userId = "auth0|123",
                version = "1.0",
                jwtToken = "test-token",
                inputs = listOf("input1"),
                outputs = listOf("output1"),
            )
        val service = RedisRunnerServiceProducer(redisTemplate, runnerStreamKey, testStreamKey)

        // When
        service.publishTestEvent(testMessage)

        // Then
        verify(redisTemplate).opsForStream<String, String>()
        verify(streamOperations).add(any<ObjectRecord<String, String>>())
    }

    @Test
    fun `publishFormatEvent should publish format message to redis stream`() {
        // Given
        val snippetMessage =
            SnippetMessage(
                snippetId = 1L,
                userId = "auth0|123",
                version = "1.0",
                jwtToken = "test-token",
            )
        val service = RedisRunnerServiceProducer(redisTemplate, runnerStreamKey, testStreamKey)

        // When
        service.publishFormatEvent(snippetMessage)

        // Then
        verify(redisTemplate).opsForStream<String, String>()
        verify(streamOperations).add(any<MapRecord<String, String, String>>())
    }

    @Test
    fun `publishLintEvent should publish lint message to redis stream`() {
        // Given
        val snippetMessage =
            SnippetMessage(
                snippetId = 1L,
                userId = "auth0|123",
                version = "1.0",
                jwtToken = "test-token",
            )
        val service = RedisRunnerServiceProducer(redisTemplate, runnerStreamKey, testStreamKey)

        // When
        service.publishLintEvent(snippetMessage)

        // Then
        verify(redisTemplate).opsForStream<String, String>()
        verify(streamOperations).add(any<MapRecord<String, String, String>>())
    }
}
