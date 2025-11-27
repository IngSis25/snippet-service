package snippets.dto.response

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.model.Test as TestModel

class TestDTOTest {
    @Test
    fun `constructor should create TestDTO from Test model`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "owner@test.com",
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
        val testDTO = TestDTO(test)

        // Then
        testDTO.id shouldBeEqualTo 1L
        testDTO.name shouldBeEqualTo "Test 1"
        testDTO.input shouldBeEqualTo listOf("input1", "input2")
        testDTO.output shouldBeEqualTo listOf("output1")
    }
}
