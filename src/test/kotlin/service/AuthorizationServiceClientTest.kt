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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto
import snippets.service.AuthUserDTO
import snippets.service.AuthorizationServiceClient

@ExtendWith(MockitoExtension::class)
class AuthorizationServiceClientTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @BeforeEach
    fun setUp() {
        authorizationServiceClient = AuthorizationServiceClient(restTemplate, "http://auth-service")
    }

    @Test
    fun `validate should return user id when token is valid`() {
        // Given
        val response = ResponseEntity.ok("auth0|123")
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(response)

        // When
        val result = authorizationServiceClient.validate("Bearer token")

        // Then
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == "auth0|123")
    }

    @Test
    fun `validate should return unauthorized when token is invalid`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenThrow(RuntimeException("Invalid token"))

        // When
        val result = authorizationServiceClient.validate("Bearer invalid")

        // Then
        assert(result.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `getSnippetsOfUser should return snippets when user exists`() {
        // Given
        val snippets =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Guest"),
            )
        val validateResponse = ResponseEntity.ok("auth0|123")
        val snippetsResponse = ResponseEntity.ok(snippets)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(validateResponse)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(snippetsResponse)

        // When
        val result = authorizationServiceClient.getSnippetsOfUser("token", "user123")

        // Then
        assert(result.size == 2)
    }

    @Test
    fun `getSnippetsOfUser should return empty list when auth0Id is null`() {
        // Given
        val validateResponse: ResponseEntity<String> = ResponseEntity.ok("")

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(validateResponse)

        // When
        val result = authorizationServiceClient.getSnippetsOfUser("token", "user123")

        // Then
        assert(result.isEmpty())
    }

    @Test
    fun `getSnippetsOfUser should return empty list on error`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenThrow(RuntimeException("Error"))

        // When
        val result = authorizationServiceClient.getSnippetsOfUser("token", "user123")

        // Then
        assert(result.isEmpty())
    }

    @Test
    fun `checkIfOwner should return true when user is owner`() {
        // Given
        val response = ResponseEntity.ok("User is the owner of the snippet")
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(response)

        // When
        val result = authorizationServiceClient.checkIfOwner(1L, "user@example.com", "token")

        // Then
        assert(result == true)
    }

    @Test
    fun `checkIfOwner should return false when user is not owner`() {
        // Given
        val response = ResponseEntity.ok("User is not the owner")
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(response)

        // When
        val result = authorizationServiceClient.checkIfOwner(1L, "user@example.com", "token")

        // Then
        assert(result == false)
    }

    @Test
    fun `checkIfOwner should return false on error`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenThrow(RuntimeException("Error"))

        // When
        val result = authorizationServiceClient.checkIfOwner(1L, "user@example.com", "token")

        // Then
        assert(result == false)
    }

    @Test
    fun `shareSnippet should return bad request when sharing with self`() {
        // Given
        val snippet = FullSnippet()

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "user@example.com",
                "user@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `shareSnippet should return forbidden when user is not owner`() {
        // Given
        val snippet = FullSnippet()
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("User is not the owner"))

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "user@example.com",
                "other@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun `getUserRoleForSnippet should return role when found`() {
        // Given
        val snippets = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        val validateResponse = ResponseEntity.ok("auth0|123")
        val snippetsResponse = ResponseEntity.ok(snippets)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(validateResponse)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(snippetsResponse)

        // When
        val result = authorizationServiceClient.getUserRoleForSnippet("token", 1L)

        // Then
        assert(result == "Owner")
    }

    @Test
    fun `getUserRoleForSnippet should return null when not found`() {
        // Given
        val snippets = listOf<SnippetUserDto>()
        val validateResponse = ResponseEntity.ok("auth0|123")
        val snippetsResponse = ResponseEntity.ok(snippets)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(validateResponse)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(snippetsResponse)

        // When
        val result = authorizationServiceClient.getUserRoleForSnippet("token", 1L)

        // Then
        assert(result == null)
    }

    @Test
    fun `searchUsers should return users when search is not blank`() {
        // Given
        val users =
            listOf(
                AuthUserDTO(id = "auth0|1", email = "user1@example.com"),
                AuthUserDTO(id = "auth0|2", email = "user2@example.com"),
            )
        val response = ResponseEntity.ok(users)

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.GET),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(response)

        // When
        val result = authorizationServiceClient.searchUsers("user", "token")

        // Then
        assert(result.size == 2)
    }

    @Test
    fun `searchUsers should return empty list when search is blank`() {
        // When
        val result = authorizationServiceClient.searchUsers("", "token")

        // Then
        assert(result.isEmpty())
    }
}
