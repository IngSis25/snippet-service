package security

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

/**
 * Configuración de seguridad OAuth2 Resource Server.
 *
 * Esta configuración permite que el snippet-service valide JWT tokens emitidos por Auth0.
 *
 * Funcionalidades:
 * - Configura Spring Security para usar OAuth2 Resource Server
 * - Valida JWT tokens contra el issuer de Auth0
 * - Valida el audience del token
 * - Requiere autenticación para todas las rutas (excepto las que se configuren como públicas)
 * - Deshabilita CSRF (ya que usamos JWT stateless)
 * - Habilita CORS
 *
 * Esta configuración NO se carga en el perfil "test" para evitar problemas en los tests.
 * También requiere que las propiedades de OAuth2 estén configuradas.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver.jwt",
    name = ["issuer-uri"],
    matchIfMissing = false,
)
class OAuth2ResourceServerSecurityConfiguration(
    @Value("\${auth0.audience}") val audience: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") val issuer: String,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authz ->
                // Por ahora, todas las rutas requieren autenticación
                // Puedes agregar excepciones aquí si necesitas endpoints públicos
                authz.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(withDefaults())
            }
            .cors(withDefaults())
            .csrf { csrf ->
                csrf.disable() // Deshabilitamos CSRF porque usamos JWT stateless
            }
        return http.build()
    }

    /**
     * Configura el JwtDecoder para validar tokens JWT.
     *
     * - Obtiene las claves públicas del issuer (Auth0) automáticamente
     * - Valida que el issuer sea correcto
     * - Valida que el audience sea correcto usando AudienceValidator
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build()

        // Validador que verifica el issuer
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)

        // Validador personalizado que verifica el audience
        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)

        // Combinamos ambos validadores
        val withAudience: OAuth2TokenValidator<Jwt> =
            DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)

        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }
}
