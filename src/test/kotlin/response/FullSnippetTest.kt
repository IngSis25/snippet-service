package snippets.dto.response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class FullSnippetTest {
    @Test
    fun `constructor with snippet content and errors should create FullSnippet`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(id = 1L, name = "Test", owner = "owner@test.com", status = Compliance.SUCCESS, language = language)
        val content = "print('Hello')"
        val errors = listOf("error1", "error2")

        // When
        val fullSnippet = FullSnippet(snippet, content, errors)

        // Then
        fullSnippet.id shouldBeEqualTo 1L
        fullSnippet.name shouldBeEqualTo "Test"
        fullSnippet.content shouldBeEqualTo content
        fullSnippet.errors shouldBeEqualTo errors
        fullSnippet.status shouldBeEqualTo Compliance.SUCCESS
    }

    @Test
    fun `constructor with snippet and content should create FullSnippet with empty errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(id = 1L, name = "Test", owner = "owner@test.com", status = Compliance.PENDING, language = language)
        val content = "print('Hello')"

        // When
        val fullSnippet = FullSnippet(snippet, content)

        // Then
        fullSnippet.id shouldBeEqualTo 1L
        fullSnippet.name shouldBeEqualTo "Test"
        fullSnippet.content shouldBeEqualTo content
        fullSnippet.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `default constructor should create empty FullSnippet`() {
        // When
        val fullSnippet = FullSnippet()

        // Then
        fullSnippet.id shouldBeEqualTo 0L
        fullSnippet.name shouldBeEqualTo ""
        fullSnippet.content shouldBeEqualTo ""
        fullSnippet.errors.isEmpty() shouldBeEqualTo true
    }
}
