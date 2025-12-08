package snippets.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import snippets.service.AuthUserDTO
import snippets.service.AuthorizationServiceClient

@RestController
@RequestMapping("/api/auth0")
class Auth0ProxyController(
    private val authorizationServiceClient: AuthorizationServiceClient,
) {
    @GetMapping("/users")
    fun searchUsers(
        @RequestParam(required = false, defaultValue = "") search: String,
        @RequestHeader("Authorization") authHeader: String?,
    ): ResponseEntity<List<AuthUserDTO>> {
        if (authHeader.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val users = authorizationServiceClient.searchUsers(search, authHeader)
        return ResponseEntity.ok(users)
    }
}
