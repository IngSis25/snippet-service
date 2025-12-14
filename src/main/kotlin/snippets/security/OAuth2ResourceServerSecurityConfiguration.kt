package snippets.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@Profile("!test")
@ConditionalOnProperty(
    name = ["spring.security.oauth2.resourceserver.jwt.issuer-uri"],
    matchIfMissing = false,
)
class OAuth2ResourceServerSecurityConfiguration(
    @Value("\${auth0.audience:}") val audience: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") val issuer: String,
) {
    private fun normalizeIssuer(issuer: String): String = issuer.trim().removeSuffix("/")

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/snippets/test-error").permitAll() // Endpoint de prueba para New Relic
                .anyRequest().authenticated()
        }
            .oauth2ResourceServer { it.jwt(withDefaults()) }
            .cors {
                it.disable()
            }
            .csrf {
                it.disable()
            }
        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        require(issuer.isNotBlank()) { "Issuer URI must not be blank" }
        require(audience.isNotBlank()) { "Audience must not be blank" }

        val normalizedIssuer = issuer.trim().removeSuffix("/")

        val jwtDecoder = NimbusJwtDecoder.withIssuerLocation(normalizedIssuer).build()

        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(normalizedIssuer)
        val withAudience: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)

        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }
}
