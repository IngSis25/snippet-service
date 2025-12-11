package enums

import org.junit.jupiter.api.Test
import snippets.enums.RulesType

class RulesTypeTest {
    @Test
    fun `RulesType should have FORMATTER and LINTER values`() {
        // When/Then
        assert(RulesType.FORMATTER.name == "FORMATTER")
        assert(RulesType.LINTER.name == "LINTER")
    }

    @Test
    fun `RulesType should be enum`() {
        // When/Then
        assert(RulesType.values().size == 2)
        assert(RulesType.values().contains(RulesType.FORMATTER))
        assert(RulesType.values().contains(RulesType.LINTER))
    }
}
