package model

import org.junit.jupiter.api.Test
import snippets.model.Compliance

class ComplianceTest {
    @Test
    fun `Compliance should have all enum values`() {
        // When/Then
        assert(Compliance.PENDING.name == "PENDING")
        assert(Compliance.SUCCESS.name == "SUCCESS")
        assert(Compliance.FAILED.name == "FAILED")
        assert(Compliance.NOT_COMPLIANT.name == "NOT_COMPLIANT")
    }

    @Test
    fun `Compliance should have all four values`() {
        // When/Then
        assert(Compliance.values().size == 4)
        assert(Compliance.values().contains(Compliance.PENDING))
        assert(Compliance.values().contains(Compliance.SUCCESS))
        assert(Compliance.values().contains(Compliance.FAILED))
        assert(Compliance.values().contains(Compliance.NOT_COMPLIANT))
    }
}
