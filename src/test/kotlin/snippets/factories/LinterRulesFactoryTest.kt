package snippets.factories

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LinterRulesFactoryTest {
    private val factory = LinterRulesFactory()

    @Test
    fun `getAvailableRules should return rules for version 1_0`() {
        // When
        val rules = factory.getAvailableRules("1.0")

        // Then
        rules.size shouldBeEqualTo 2
        rules.map { it.id } shouldContain "UnusedVariableCheck"
        rules.map { it.id } shouldContain "NamingFormatCheck"
    }

    @Test
    fun `getAvailableRules should return rules for version 1_1`() {
        // When
        val rules = factory.getAvailableRules("1.1")

        // Then
        rules.size shouldBeEqualTo 4
        rules.map { it.id } shouldContain "UnusedVariableCheck"
        rules.map { it.id } shouldContain "NamingFormatCheck"
        rules.map { it.id } shouldContain "PrintUseCheck"
        rules.map { it.id } shouldContain "ReadInputCheck"
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
        ruleIds.size shouldBeEqualTo 2
        ruleIds shouldContain "UnusedVariableCheck"
        ruleIds shouldContain "NamingFormatCheck"
    }

    @Test
    fun `getAvailableRuleIds should return rule IDs for version 1_1`() {
        // When
        val ruleIds = factory.getAvailableRuleIds("1.1")

        // Then
        ruleIds.size shouldBeEqualTo 4
        ruleIds shouldContain "UnusedVariableCheck"
        ruleIds shouldContain "NamingFormatCheck"
        ruleIds shouldContain "PrintUseCheck"
        ruleIds shouldContain "ReadInputCheck"
    }

    @Test
    fun `rules for version 1_0 should have UnusedVariableCheck active by default`() {
        // When
        val rules = factory.getAvailableRules("1.0")

        // Then
        val unusedVarRule = rules.find { it.id == "UnusedVariableCheck" }
        assert(unusedVarRule != null)
        unusedVarRule!!.isActive shouldBeEqualTo true
        unusedVarRule.value shouldBeEqualTo null
    }

    @Test
    fun `rules for version 1_0 should have NamingFormatCheck with camelCase default`() {
        // When
        val rules = factory.getAvailableRules("1.0")

        // Then
        val namingRule = rules.find { it.id == "NamingFormatCheck" }
        assert(namingRule != null)
        namingRule!!.isActive shouldBeEqualTo false
        namingRule.value shouldBeEqualTo "camelCase"
    }

    @Test
    fun `rules for version 1_1 should have UnusedVariableCheck active by default`() {
        // When
        val rules = factory.getAvailableRules("1.1")

        // Then
        val unusedVarRule = rules.find { it.id == "UnusedVariableCheck" }
        assert(unusedVarRule != null)
        unusedVarRule!!.isActive shouldBeEqualTo true
    }
}
