package snippets.service

import org.springframework.http.ResponseEntity
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto

/**
 * Interfaz para el cliente HTTP del authorization-service.
 * Define los métodos para comunicarse con el servicio de autorización.
 */
interface AuthorizationServiceClientRoutes {
    fun checkIfOwner(
        snippetId: Long,
        email: String,
        token: String,
    ): Boolean

    fun addSnippetToUser(
        token: String,
        email: String,
        snippetId: Long,
        role: String,
    ) {
        println("DEBUG TOKEN addSnippetToUser = '$token'")
    }

    fun getSnippetsOfUser(
        token: String,
        userId: String,
    ): List<SnippetUserDto>

    fun shareSnippet(
        token: String,
        snippetId: Long,
        fromEmail: String,
        toEmail: String,
        snippet: FullSnippet,
    ): ResponseEntity<FullSnippet>

    fun validate(token: String): ResponseEntity<String>
}
