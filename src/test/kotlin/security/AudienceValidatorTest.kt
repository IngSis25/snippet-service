package snippets.security

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class AudienceValidatorTest {
    @Test
    fun `validate should return success when audience matches`() {
        // Given
        val audience = "test-audience"
        val jwt = createJwtWithAudience(listOf("test-audience", "other-audience"))
        val validator = AudienceValidator(audience)

        // When
        val result = validator.validate(jwt)

        // Then
        assert(!result.hasErrors())
    }

    @Test
    fun `validate should return failure when audience does not match`() {
        // Given
        val audience = "test-audience"
        val jwt = createJwtWithAudience(listOf("other-audience"))
        val validator = AudienceValidator(audience)

        // When
        val result = validator.validate(jwt)

        // Then
        assert(result.hasErrors())
        assert(result.errors.any { it.errorCode == "invalid_token" })
    }

    @Test
    fun `validate should return failure when audience list is empty`() {
        // Given
        val audience = "test-audience"
        val jwt = createJwtWithAudience(emptyList())
        val validator = AudienceValidator(audience)

        // When
        val result = validator.validate(jwt)

        // Then
        assert(result.hasErrors())
    }

    private fun createJwtWithAudience(audience: List<String>): Jwt {
        val headers = mapOf<String, Any>("alg" to "RS256")
        val claims =
            mapOf<String, Any>(
                "sub" to "user123",
                "aud" to audience,
                "iat" to Instant.now().epochSecond,
                "exp" to Instant.now().plusSeconds(3600).epochSecond,
            )
        return Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims)
    }
}
