package snippets.factories

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FormatterRulesFactoryTest {
    private val factory = FormatterRulesFactory()

    @Test
    fun `getAvailableRules should return rules for version 1_0`() {
        // When
        val rules = factory.getAvailableRules("1.0")

        // Then
        rules.size shouldBeEqualTo 7
        rules.map { it.id } shouldContain "space_before_colon"
        rules.map { it.id } shouldContain "space_after_colon"
        rules.map { it.id } shouldContain "newline_after_println"
        rules.map { it.id } shouldContain "newline_before_println"
        rules.map { it.id } shouldContain "space_around_equals"
        rules.map { it.id } shouldContain "no_space_around_equals"
        rules.map { it.id } shouldContain "single_space_separation"
    }

    @Test
    fun `getAvailableRules should return rules for version 1_1`() {
        // When
        val rules = factory.getAvailableRules("1.1")

        // Then
        rules.size shouldBeEqualTo 11
        rules.map { it.id } shouldContain "space_before_colon"
        rules.map { it.id } shouldContain "space_after_colon"
        rules.map { it.id } shouldContain "newline_after_println"
        rules.map { it.id } shouldContain "newline_before_println"
        rules.map { it.id } shouldContain "space_around_equals"
        rules.map { it.id } shouldContain "no_space_around_equals"
        rules.map { it.id } shouldContain "number_of_spaces_indentation"
        rules.map { it.id } shouldContain "same_line_for_if_brace"
        rules.map { it.id } shouldContain "same_line_for_else_brace"
        rules.map { it.id } shouldContain "new_line_for_if_brace"
        rules.map { it.id } shouldContain "single_space_separation"
    }

    @Test
    fun `getAvailableRules should throw error for unsupported version`() {
        // When/Then
        assertThrows<IllegalStateException> {
            factory.getAvailableRules("2.0")
        }
    }

    @Test
    fun `getAvailableRuleIds should return rule IDs for version 1_0`() {
        // When
        val ruleIds = factory.getAvailableRuleIds("1.0")

        // Then
        ruleIds.size shouldBeEqualTo 7
        ruleIds shouldContain "space_before_colon"
        ruleIds shouldContain "single_space_separation"
    }

    @Test
    fun `getAvailableRuleIds should return rule IDs for version 1_1`() {
        // When
        val ruleIds = factory.getAvailableRuleIds("1.1")

        // Then
        ruleIds.size shouldBeEqualTo 11
        ruleIds shouldContain "number_of_spaces_indentation"
        ruleIds shouldContain "same_line_for_if_brace"
    }

    @Test
    fun `rules for version 1_0 should have correct default values`() {
        // When
        val rules = factory.getAvailableRules("1.0")

        // Then
        rules.forEach { rule ->
            rule.isActive shouldBeEqualTo false
            if (rule.id != "number_of_spaces_indentation") {
                rule.value shouldBeEqualTo null
            }
        }
    }

    @Test
    fun `rules for version 1_1 should have number_of_spaces_indentation with default value 2`() {
        // When
        val rules = factory.getAvailableRules("1.1")

        // Then
        val indentationRule = rules.find { it.id == "number_of_spaces_indentation" }
        assert(indentationRule != null)
        indentationRule!!.value shouldBeEqualTo 2
    }
}
