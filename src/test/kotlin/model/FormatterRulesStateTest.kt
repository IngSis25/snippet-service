package model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.enums.RulesType
import snippets.model.FormatterRulesState

class FormatterRulesStateTest {
    @Test
    fun `FormatterRulesState should have all properties`() {
        // When
        val state =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = listOf("rule1", "rule2"),
                optionsJson = mapOf("rule1" to "value1"),
                configText = "config text",
                configFormat = "json",
            )

        // Then
        state.type shouldBeEqualTo RulesType.FORMATTER
        state.ownerId shouldBeEqualTo "user123"
        state.enabledJson.size shouldBeEqualTo 2
        state.optionsJson?.size shouldBeEqualTo 1
        state.configText shouldBeEqualTo "config text"
        state.configFormat shouldBeEqualTo "json"
    }

    @Test
    fun `FormatterRulesState should handle null values`() {
        // When
        val state =
            FormatterRulesState(
                id = null,
                type = RulesType.LINTER,
                ownerId = null,
                enabledJson = emptyList(),
                optionsJson = null,
                configText = null,
                configFormat = null,
            )

        // Then
        state.type shouldBeEqualTo RulesType.LINTER
        state.ownerId shouldBeEqualTo null
        state.enabledJson.isEmpty() shouldBeEqualTo true
        state.optionsJson shouldBeEqualTo null
    }
}
