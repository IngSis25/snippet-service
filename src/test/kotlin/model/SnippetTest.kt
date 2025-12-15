package model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetTest {
    @Test
    fun `Snippet should have all properties`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")

        // When
        val snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.SUCCESS,
                language = language,
            )

        // Then
        snippet.id shouldBeEqualTo 1L
        snippet.name shouldBeEqualTo "Test Snippet"
        snippet.owner shouldBeEqualTo "test@example.com"
        snippet.status shouldBeEqualTo Compliance.SUCCESS
        snippet.language.id shouldBeEqualTo 1L
    }

    @Test
    fun `Snippet should have default constructor`() {
        // When
        val snippet = Snippet()

        // Then
        snippet.id shouldBeEqualTo 0L
        snippet.name shouldBeEqualTo ""
        snippet.owner shouldBeEqualTo ""
        snippet.status shouldBeEqualTo Compliance.PENDING
    }

    @Test
    fun `Snippet toString should return formatted string`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )

        // When
        val result = snippet.toString()

        // Then
        assert(result.contains("id=1"))
        assert(result.contains("name='Test'"))
        assert(result.contains("owner='user'"))
    }
}
