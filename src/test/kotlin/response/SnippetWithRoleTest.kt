package snippets.dto.response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetWithRoleTest {
    @Test
    fun `constructor should create SnippetWithRole`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(id = 1L, name = "Test", owner = "owner@test.com", status = Compliance.SUCCESS, language = language)
        val role = "Owner"

        // When
        val result = SnippetWithRole(snippet, role)

        // Then
        result.id shouldBeEqualTo 1L
        result.name shouldBeEqualTo "Test"
        result.role shouldBeEqualTo role
        result.status shouldBeEqualTo Compliance.SUCCESS
        result.language shouldBeEqualTo "PrintScript"
    }

    @Test
    fun `default constructor should create empty SnippetWithRole`() {
        // When
        val result = SnippetWithRole()

        // Then
        result.id shouldBeEqualTo 0L
        result.name shouldBeEqualTo ""
        result.role shouldBeEqualTo "Default"
    }
}
