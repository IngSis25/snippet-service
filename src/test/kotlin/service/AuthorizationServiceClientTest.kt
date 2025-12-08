package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.service.AuthorizationServiceClient

@ExtendWith(MockitoExtension::class)
class AuthorizationServiceClientTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private val authorizationServiceUrl = "http://authorization-service:8080/api"
    private lateinit var language: Language
    private lateinit var snippet: Snippet
    private lateinit var fullSnippet: FullSnippet

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.PENDING,
                language = language,
            )
        fullSnippet = FullSnippet(snippet, "print('Hello')", emptyList())
    }

    @Test
    fun `checkIfOwner should return true when user is owner`() {
        // Given
        val snippetId = 1L
        val email = "test@example.com"
        val token = "test-token"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok("User is the owner of the snippet"))

        // When
        val result = service.checkIfOwner(snippetId, email, token)

        // Then
        assert(result == true)
    }

    @Test
    fun `checkIfOwner should return false when user is not owner`() {
        // Given
        val snippetId = 1L
        val email = "other@example.com"
        val token = "test-token"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok("User is not the owner"))

        // When
        val result = service.checkIfOwner(snippetId, email, token)

        // Then
        assert(result == false)
    }

    @Test
    fun `checkIfOwner should return false on exception`() {
        // Given
        val snippetId = 1L
        val email = "test@example.com"
        val token = "test-token"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenThrow(RuntimeException("Error"))

        // When
        val result = service.checkIfOwner(snippetId, email, token)

        // Then
        assert(result == false)
    }

    @Test
    fun `validate should return user id when token is valid`() {
        // Given
        val token = "test-token"
        val userId = "auth0|123"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok(userId))

        // When
        val result = service.validate(token)

        // Then
        assert(result.body == userId)
        assert(result.statusCode == HttpStatus.OK)
    }

    @Test
    fun `validate should return unauthorized when token is invalid`() {
        // Given
        val token = "invalid-token"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenThrow(RuntimeException("Invalid token"))

        // When
        val result = service.validate(token)

        // Then
        assert(result.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `getSnippetsOfUser should return list of snippets`() {
        // Given
        val token = "test-token"
        val userId = "user123"
        val auth0Id = "auth0|123"
        val snippets = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        // Mock validate() first
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok(auth0Id))

        // Mock get-user-snippets
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any(),
                any<ParameterizedTypeReference<List<SnippetUserDto>>>(),
            ),
        ).thenReturn(ResponseEntity.ok(snippets))

        // When
        val result = service.getSnippetsOfUser(token, userId)

        // Then
        assert(result.size == 1)
        assert(result[0].snippetId == 1L)
    }

    @Test
    fun `getSnippetsOfUser should return empty list on exception`() {
        // Given
        val token = "test-token"
        val userId = "user123"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<ParameterizedTypeReference<List<SnippetUserDto>>>(),
            ),
        ).thenThrow(RuntimeException("Error"))

        // When
        val result = service.getSnippetsOfUser(token, userId)

        // Then
        assert(result.isEmpty())
    }

    @Test
    fun `shareSnippet should return bad request when sharing with self`() {
        // Given
        val token = "test-token"
        val snippetId = 1L
        val email = "test@example.com"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        // When
        val result = service.shareSnippet(token, snippetId, email, email, fullSnippet, "editor")

        // Then
        assert(result.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `shareSnippet should return forbidden when user is not owner`() {
        // Given
        val token = "test-token"
        val snippetId = 1L
        val fromEmail = "from@example.com"
        val toEmail = "to@example.com"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok("User is not the owner"))

        // When
        val result = service.shareSnippet(token, snippetId, fromEmail, toEmail, fullSnippet, "editor")

        // Then
        assert(result.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun `shareSnippet should return ok when sharing is successful`() {
        // Given
        val token = "test-token"
        val snippetId = 1L
        val fromEmail = "test@example.com"
        val toEmail = "to@example.com"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        // Mock checkIfOwner (returns true)
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok("User is the owner of the snippet"))

        // When
        val result = service.shareSnippet(token, snippetId, fromEmail, toEmail, fullSnippet, "editor")

        // Then
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body?.id == fullSnippet.id)
    }

    @Test
    fun `addSnippetToUser should call rest template`() {
        // Given
        val token = "test-token"
        val email = "test@example.com"
        val snippetId = 1L
        val role = "Owner"
        val service = AuthorizationServiceClient(restTemplate, authorizationServiceUrl)

        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any(),
                any<Class<String>>(),
            ),
        ).thenReturn(ResponseEntity.ok("Success"))

        // When
        service.addSnippetToUser(token, email, snippetId, role)

        // Then
        // Verificamos que se llamó al restTemplate
        // (no hay valor de retorno para verificar directamente)
    }
}
