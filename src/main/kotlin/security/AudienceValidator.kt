package security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Validador personalizado para verificar que el JWT contiene el audience requerido.
 * Auth0 incluye el audience en el token JWT, y este validador asegura que coincida
 * con el audience configurado en la aplicación.
 */
class AudienceValidator(private val audience: String) : OAuth2TokenValidator<Jwt> {
    override fun validate(jwtToken: Jwt): OAuth2TokenValidatorResult {
        return if (jwtToken.audience.contains(audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "The required audience is missing", null),
            )
        }
    }
}
