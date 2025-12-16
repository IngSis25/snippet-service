package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.SnippetUserDto

class SnippetUserDtoTest {
    @Test
    fun `SnippetUserDto should have all properties`() {
        // When
        val dto = SnippetUserDto(snippetId = 1L, role = "Owner")

        // Then
        dto.snippetId shouldBeEqualTo 1L
        dto.role shouldBeEqualTo "Owner"
    }
}
