package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.FullSnippet
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class FullSnippetTest {
    @Test
    fun `FullSnippet should have all properties from snippet and content`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.SUCCESS,
                language = language,
            )

        // When
        val fullSnippet = FullSnippet(snippet, "print('hello')", listOf("error1"))

        // Then
        fullSnippet.id shouldBeEqualTo 1L
        fullSnippet.name shouldBeEqualTo "Test Snippet"
        fullSnippet.owner shouldBeEqualTo "test@example.com"
        fullSnippet.language shouldBeEqualTo "PrintScript"
        fullSnippet.extension shouldBeEqualTo "ps"
        fullSnippet.version shouldBeEqualTo "1.0"
        fullSnippet.content shouldBeEqualTo "print('hello')"
        fullSnippet.status shouldBeEqualTo Compliance.SUCCESS
        fullSnippet.errors.size shouldBeEqualTo 1
    }

    @Test
    fun `FullSnippet should have default constructor`() {
        // When
        val fullSnippet = FullSnippet()

        // Then
        fullSnippet.id shouldBeEqualTo 0L
        fullSnippet.name shouldBeEqualTo ""
        fullSnippet.content shouldBeEqualTo ""
        fullSnippet.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `FullSnippet should have constructor with snippet and content only`() {
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
        val fullSnippet = FullSnippet(snippet, "content")

        // Then
        fullSnippet.content shouldBeEqualTo "content"
        fullSnippet.errors.isEmpty() shouldBeEqualTo true
    }
}
