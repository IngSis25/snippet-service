package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.SnippetDetailDto
import snippets.model.Compliance

class SnippetDetailDtoTest {
    @Test
    fun `SnippetDetailDto should have all properties`() {
        // When
        val dto =
            SnippetDetailDto(
                id = 1L,
                name = "Test Snippet",
                owner = "user@example.com",
                language = "PrintScript",
                extension = "ps",
                version = "1.0",
                content = "print('hello')",
                compliance = Compliance.SUCCESS,
                lintCount = 0,
                isValid = true,
                warnings = emptyList(),
            )

        // Then
        dto.id shouldBeEqualTo 1L
        dto.name shouldBeEqualTo "Test Snippet"
        dto.owner shouldBeEqualTo "user@example.com"
        dto.language shouldBeEqualTo "PrintScript"
        dto.extension shouldBeEqualTo "ps"
        dto.version shouldBeEqualTo "1.0"
        dto.content shouldBeEqualTo "print('hello')"
        dto.compliance shouldBeEqualTo Compliance.SUCCESS
        dto.lintCount shouldBeEqualTo 0
        dto.isValid shouldBeEqualTo true
        dto.warnings.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `SnippetDetailDto should handle warnings`() {
        // When
        val dto =
            SnippetDetailDto(
                id = 1L,
                name = "Test",
                owner = "user",
                language = "PrintScript",
                extension = "ps",
                version = "1.0",
                content = "code",
                compliance = Compliance.FAILED,
                lintCount = 2,
                isValid = false,
                warnings = listOf("warning1", "warning2"),
            )

        // Then
        dto.warnings.size shouldBeEqualTo 2
        dto.lintCount shouldBeEqualTo 2
        dto.isValid shouldBeEqualTo false
    }
}
