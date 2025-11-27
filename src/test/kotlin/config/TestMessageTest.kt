package snippets.config

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TestMessageTest {
    @Test
    fun `constructor should create TestMessage`() {
        // Given
        val testId = 1L
        val snippetId = 2L
        val userId = 3L
        val version = "1.0"
        val jwtToken = "test-token"
        val inputs = listOf("input1", "input2")
        val outputs = listOf("output1")

        // When
        val message = TestMessage(testId, snippetId, userId, version, jwtToken, inputs, outputs)

        // Then
        message.testId shouldBeEqualTo testId
        message.snippetId shouldBeEqualTo snippetId
        message.userId shouldBeEqualTo userId
        message.version shouldBeEqualTo version
        message.jwtToken shouldBeEqualTo jwtToken
        message.inputs shouldBeEqualTo inputs
        message.outputs shouldBeEqualTo outputs
    }
}
