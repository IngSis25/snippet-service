package snippets.config

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SnippetMessageTest {
    @Test
    fun `constructor should create SnippetMessage`() {
        // Given
        val snippetId = 1L
        val userId = "auth0|123"
        val version = "1.0"
        val jwtToken = "test-token"

        // When
        val message = SnippetMessage(snippetId, userId, version, jwtToken)

        // Then
        message.snippetId shouldBeEqualTo snippetId
        message.userId shouldBeEqualTo userId
        message.version shouldBeEqualTo version
        message.jwtToken shouldBeEqualTo jwtToken
    }
}
