package request

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.request.Rule

class RuleTest {
    @Test
    fun `Rule should have all properties`() {
        // When
        val rule =
            Rule(
                id = "rule1",
                name = "rule1",
                isActive = true,
                value = "test",
            )

        // Then
        rule.id shouldBeEqualTo "rule1"
        rule.name shouldBeEqualTo "rule1"
        rule.isActive shouldBeEqualTo true
        rule.value shouldBeEqualTo "test"
    }

    @Test
    fun `Rule should handle null value`() {
        // When
        val rule =
            Rule(
                id = "rule2",
                name = "rule2",
                isActive = false,
                value = null,
            )

        // Then
        rule.value shouldBeEqualTo null
        rule.isActive shouldBeEqualTo false
    }

    @Test
    fun `Rule should handle numeric value`() {
        // When
        val rule =
            Rule(
                id = "rule3",
                name = "number_of_spaces_indentation",
                isActive = true,
                value = 2,
            )

        // Then
        rule.value shouldBeEqualTo 2
    }
}
