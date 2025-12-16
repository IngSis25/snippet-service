package model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.model.Test as TestModel

class TestModelTest {
    @Test
    fun `Test should have all properties`() {
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

        // When
        val test =
            TestModel(
                id = 1L,
                name = "Test 1",
                input = listOf("input1", "input2"),
                output = listOf("output1"),
                snippet = snippet,
            )

        // Then
        test.id shouldBeEqualTo 1L
        test.name shouldBeEqualTo "Test 1"
        test.input.size shouldBeEqualTo 2
        test.output.size shouldBeEqualTo 1
        test.snippet.id shouldBeEqualTo 1L
    }

    @Test
    fun `Test should have default constructor`() {
        // When
        val test = TestModel()

        // Then
        test.id shouldBeEqualTo 0L
        test.name shouldBeEqualTo ""
        test.input.isEmpty() shouldBeEqualTo true
        test.output.isEmpty() shouldBeEqualTo true
    }
}
