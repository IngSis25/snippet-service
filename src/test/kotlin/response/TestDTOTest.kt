package response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.response.TestDTO
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.model.Test as TestModel

class TestDTOTest {
    @Test
    fun `TestDTO should have all properties from test`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.PENDING,
                language = language,
            )
        val test =
            TestModel(
                id = 1L,
                name = "Test 1",
                input = listOf("input1", "input2"),
                output = listOf("output1"),
                snippet = snippet,
            )

        // When
        val dto = TestDTO(test)

        // Then
        dto.id shouldBeEqualTo 1L
        dto.name shouldBeEqualTo "Test 1"
        dto.input.size shouldBeEqualTo 2
        dto.output.size shouldBeEqualTo 1
    }
}
