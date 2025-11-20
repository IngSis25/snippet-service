package service

import dto.response.FullSnippet
import dto.response.SnippetUserDto
import org.springframework.http.ResponseEntity

/**
 * Interfaz para el cliente HTTP del authorization-service.
 * Define los métodos para comunicarse con el servicio de autorización.
 */
interface AuthorizationServiceClientRoutes {
    fun checkIfOwner(snippetId: Long, email: String, token: String): Boolean
    fun addSnippetToUser(token: String, email: String, snippetId: Long, role: String)
    fun getSnippetsOfUser(token: String, userId: String): List<SnippetUserDto>
    fun shareSnippet(token: String, snippetId: Long, fromEmail: String, toEmail: String, snippet: FullSnippet): ResponseEntity<FullSnippet>
    fun validate(token: String): ResponseEntity<Long>
}

