package snippets.dto.response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet

class SnippetDTOTest {
    @Test
    fun `constructor should create SnippetDTO from Snippet`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(id = 1L, name = "Test", owner = "owner@test.com", status = Compliance.SUCCESS, language = language)

        // When
        val snippetDTO = SnippetDTO(snippet)

        // Then
        snippetDTO.id shouldBeEqualTo 1L
        snippetDTO.name shouldBeEqualTo "Test"
        snippetDTO.owner shouldBeEqualTo "owner@test.com"
        snippetDTO.language shouldBeEqualTo "PrintScript"
        snippetDTO.extension shouldBeEqualTo "ps"
        snippetDTO.version shouldBeEqualTo "1.0"
        snippetDTO.compliance shouldBeEqualTo Compliance.SUCCESS
    }
}
