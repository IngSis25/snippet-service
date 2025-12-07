package snippets.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto

/**
 * Cliente HTTP para comunicarse con el authorization-service.
 * Este servicio solo hace llamadas HTTP, no maneja lógica de permisos.
 */
@Service
class AuthorizationServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${spring.authorization.service.url}") private val authorizationServiceUrl: String,
) : AuthorizationServiceClientRoutes {
    /**
     * Nuevo contrato:
     * - Endpoint: POST /api/user/add-snippet/{snippetId}
     * - El usuario se identifica por el token (claim "sub").
     * - El body solo lleva { "role": "Owner" | "Guest" | ... }.
     *
     * El parámetro email ya no se usa, pero lo dejamos en la firma
     * para no romper código que todavía lo pasa.
     */
    override fun addSnippetToUser(
        token: String,
        email: String,
        snippetId: Long,
        role: String,
    ) {
        val body: Map<String, Any> = mapOf("role" to role)
        val headers = getJsonAuthorizedHeaders(token)
        val entity = HttpEntity(body, headers)

        // Ahora usamos snippetId en el path, no email
        executePost(entity, "/add-snippet/$snippetId")
    }

    /**
     * Nuevo contrato:
     * - Endpoint: POST /api/user/check-owner/{snippetId}
     * - El usuario se identifica por el token (claim "sub").
     * - No hace falta mandar email ni snippetId en el body.
     *
     * El parámetro email se ignora.
     */
    override fun checkIfOwner(
        snippetId: Long,
        email: String,
        token: String,
    ): Boolean {
        val emptyBody: Map<String, Any> = emptyMap()
        val entity = HttpEntity(emptyBody, getJsonAuthorizedHeaders(token))

        return try {
            val response = executePost(entity, "/check-owner/$snippetId")
            response?.equals("User is the owner of the snippet", ignoreCase = true) == true
        } catch (e: Exception) {
            println("Error checking ownership: ${e.message}")
            false
        }
    }

    /**
     * Nuevo contrato:
     * - Primero llamamos a /api/user/validate con el token para obtener el auth0Id.
     * - Luego hacemos GET /api/user/get-user-snippets/{auth0Id}.
     *
     * El parámetro userId ya no se usa (lo sacamos del token),
     * pero lo dejamos en la firma para no romper dependencias.
     */
    override fun getSnippetsOfUser(
        token: String,
        userId: String,
    ): List<SnippetUserDto> {
        return try {
            // Primero obtener auth0Id desde /validate
            val validateResponse = validate(token)
            val auth0Id = validateResponse.body

            if (auth0Id.isNullOrBlank()) {
                println("Error getting snippets of user: auth0Id is null/blank from validate()")
                return emptyList()
            }

            val entity = HttpEntity<Void>(getJsonAuthorizedHeaders(token))

            val response =
                restTemplate.exchange(
                    "$authorizationServiceUrl/user/get-user-snippets/$auth0Id",
                    HttpMethod.GET,
                    entity,
                    object : ParameterizedTypeReference<List<SnippetUserDto>>() {},
                )
            response.body ?: emptyList()
        } catch (e: Exception) {
            println("Error getting snippets of user: ${e.message}")
            emptyList()
        }
    }

    override fun shareSnippet(
        token: String,
        snippetId: Long,
        fromEmail: String,
        toEmail: String,
        snippet: FullSnippet,
    ): ResponseEntity<FullSnippet> {
        if (fromEmail == toEmail) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Share-Status", "You can't share a snippet with yourself")
                .body(FullSnippet())
        }

        if (!checkIfOwner(snippetId, fromEmail, token)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header("Share-Status", "You are not the owner of the snippet")
                .body(FullSnippet())
        }

        // email del invitado se sigue usando solo como metadata, pero
        // la asociación real usuario-snippet se hace por token + auth0Id en el authorization-service
        addSnippetToUser(token, toEmail, snippetId, "Guest")

        return ResponseEntity
            .status(HttpStatus.OK)
            .header("Share-Status", "Snippet shared with $toEmail")
            .body(snippet)
    }

    override fun validate(token: String): ResponseEntity<String> {
        return try {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("Authorization", token)
                }
            val entity = HttpEntity<Void>(headers)

            restTemplate.exchange(
                "$authorizationServiceUrl/user/validate",
                HttpMethod.GET,
                entity,
                String::class.java,
            )
        } catch (e: Exception) {
            print("VALIDATE -> Error validating token: $e")
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    private fun getJsonAuthorizedHeaders(token: String): MultiValueMap<String, String> {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", token)
        }
    }

    private fun executePost(
        entity: HttpEntity<Map<String, Any>>,
        path: String,
    ): String? {
        return try {
            val response =
                restTemplate.exchange(
                    "$authorizationServiceUrl/user$path",
                    HttpMethod.POST,
                    entity,
                    String::class.java,
                )
            response.body
        } catch (e: Exception) {
            println("Error executing POST to $path: ${e.message}")
            null
        }
    }
}
