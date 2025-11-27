package snippets.dto.response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetWithRoleAndWarningsTest {
    @Test
    fun `constructor should create SnippetWithRoleAndWarnings`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(id = 1L, name = "Test", owner = "owner@test.com", status = Compliance.SUCCESS, language = language)
        val role = "Owner"
        val warnings = listOf("warning1", "warning2")

        // When
        val result = SnippetWithRoleAndWarnings(snippet, role, warnings)

        // Then
        result.id shouldBeEqualTo 1L
        result.name shouldBeEqualTo "Test"
        result.role shouldBeEqualTo role
        result.lintWarnings shouldBeEqualTo warnings
        result.status shouldBeEqualTo Compliance.SUCCESS
        result.language shouldBeEqualTo "PrintScript"
    }

    @Test
    fun `default constructor should create empty SnippetWithRoleAndWarnings`() {
        // When
        val result = SnippetWithRoleAndWarnings()

        // Then
        result.id shouldBeEqualTo 0L
        result.name shouldBeEqualTo ""
        result.role shouldBeEqualTo "Default"
        result.lintWarnings.isEmpty() shouldBeEqualTo true
    }
}
