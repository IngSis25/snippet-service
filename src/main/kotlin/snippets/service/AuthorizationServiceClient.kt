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
    override fun addSnippetToUser(
        token: String,
        email: String,
        snippetId: Long,
        role: String,
    ) {
        val body: Map<String, Any> = mapOf("snippetId" to snippetId, "role" to role)

        val headers = getJsonAuthorizedHeaders(token)

        val entity = HttpEntity(body, headers)
        executePost(entity, "/add-snippet/$email")
    }

    override fun checkIfOwner(
        snippetId: Long,
        email: String,
        token: String,
    ): Boolean {
        val body: Map<String, Any> = mapOf("snippetId" to snippetId, "email" to email)
        val entity = HttpEntity(body, getJsonAuthorizedHeaders(token))

        return try {
            val response = executePost(entity, "/check-owner")
            response?.equals("User is the owner of the snippet", ignoreCase = true) == true
        } catch (e: Exception) {
            println("Error checking ownership: ${e.message}")
            false
        }
    }

    override fun getSnippetsOfUser(
        token: String,
        userId: String,
    ): List<SnippetUserDto> {
        val body = mapOf("userId" to userId)
        val entity = HttpEntity(body, getJsonAuthorizedHeaders(token))
        return try {
            val response =
                restTemplate.exchange(
                    "$authorizationServiceUrl/user/get-user-snippets/$userId",
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

        addSnippetToUser(token, toEmail, snippetId, "Guest")
        return ResponseEntity
            .status(HttpStatus.OK)
            .header("Share-Status", "Snippet shared with $toEmail")
            .body(snippet)
    }

    override fun validate(token: String): ResponseEntity<Long> {
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
                Long::class.java,
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
        string: String,
    ): String? {
        return restTemplate.postForObject(
            "$authorizationServiceUrl/user$string",
            entity,
            String::class.java,
        )
    }
}
