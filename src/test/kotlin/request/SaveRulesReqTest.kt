package request

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.request.Rule
import snippets.dto.request.SaveRulesReq

class SaveRulesReqTest {
    @Test
    fun `SaveRulesReq should have all properties`() {
        // Given
        val rules =
            listOf(
                Rule(id = "rule1", name = "rule1", isActive = true, value = null),
            )

        // When
        val request =
            SaveRulesReq(
                rules = rules,
                configText = "config text",
                configFormat = "json",
            )

        // Then
        request.rules.size shouldBeEqualTo 1
        request.configText shouldBeEqualTo "config text"
        request.configFormat shouldBeEqualTo "json"
    }

    @Test
    fun `SaveRulesReq should handle null configText and configFormat`() {
        // Given
        val rules = emptyList<Rule>()

        // When
        val request =
            SaveRulesReq(
                rules = rules,
                configText = null,
                configFormat = null,
            )

        // Then
        request.rules.isEmpty() shouldBeEqualTo true
        request.configText shouldBeEqualTo null
        request.configFormat shouldBeEqualTo null
    }
}
