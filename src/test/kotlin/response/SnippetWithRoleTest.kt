package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.SnippetWithRole
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetWithRoleTest {
    @Test
    fun `SnippetWithRole should have all properties from snippet and role`() {
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
        val dto = SnippetWithRole(snippet, "Owner")

        // Then
        dto.id shouldBeEqualTo 1L
        dto.name shouldBeEqualTo "Test Snippet"
        dto.owner shouldBeEqualTo "test@example.com"
        dto.language shouldBeEqualTo "PrintScript"
        dto.extension shouldBeEqualTo "ps"
        dto.version shouldBeEqualTo "1.0"
        dto.status shouldBeEqualTo Compliance.SUCCESS
        dto.role shouldBeEqualTo "Owner"
    }

    @Test
    fun `SnippetWithRole should have default constructor`() {
        // When
        val dto = SnippetWithRole()

        // Then
        dto.id shouldBeEqualTo 0L
        dto.name shouldBeEqualTo ""
        dto.role shouldBeEqualTo "Default"
    }
}
