package request

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.request.DiagnosticDto

class DiagnosticDtoTest {
    @Test
    fun `DiagnosticDto should have all properties`() {
        // When
        val dto =
            DiagnosticDto(
                code = "E001",
                message = "Error message",
                severity = "error",
                line = 10,
                column = 5,
                suggestions = emptyList(),
            )

        // Then
        dto.code shouldBeEqualTo "E001"
        dto.message shouldBeEqualTo "Error message"
        dto.severity shouldBeEqualTo "error"
        dto.line shouldBeEqualTo 10
        dto.column shouldBeEqualTo 5
        dto.suggestions.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `DiagnosticDto should handle suggestions`() {
        // When
        val dto =
            DiagnosticDto(
                code = "W001",
                message = "Warning message",
                severity = "warning",
                line = 5,
                column = 3,
                suggestions = listOf("suggestion1", "suggestion2"),
            )

        // Then
        dto.suggestions.size shouldBeEqualTo 2
        dto.suggestions[0] shouldBeEqualTo "suggestion1"
    }
}
