package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.SnippetDTO
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetDTOTest {
    @Test
    fun `SnippetDTO should have all properties from snippet`() {
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
        val dto = SnippetDTO(snippet)

        // Then
        dto.id shouldBeEqualTo 1L
        dto.name shouldBeEqualTo "Test Snippet"
        dto.owner shouldBeEqualTo "test@example.com"
        dto.language shouldBeEqualTo "PrintScript"
        dto.extension shouldBeEqualTo "ps"
        dto.version shouldBeEqualTo "1.0"
        dto.compliance shouldBeEqualTo Compliance.SUCCESS
    }
}
